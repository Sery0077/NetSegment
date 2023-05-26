package sery.vlasenko.netsegment.ui.server

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import okhttp3.internal.closeQuietly
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.data.NetworkModule
import sery.vlasenko.netsegment.domain.socket_handlers.server.*
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.model.connections.UdpConnection
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.TestResult
import sery.vlasenko.netsegment.model.test.udp.UdpPacketDisconnect
import sery.vlasenko.netsegment.model.testscripts.TestItem
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.ResourceProvider
import sery.vlasenko.netsegment.utils.Scripts
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import java.net.*
import java.util.*

class ServerViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    private val _connItem: MutableLiveData<ConnectionItem?> = MutableLiveData(null)
    val connItem: LiveData<ConnectionItem?>
        get() = this._connItem

    private val _uiState: MutableLiveData<ServerButtonState> = MutableLiveData()
    val uiState: LiveData<ServerButtonState>
        get() = _uiState

    private val _singleEvent: MutableLiveData<SingleEvent> = MutableLiveData()
    val singleEvent: LiveData<SingleEvent>
        get() = _singleEvent

    private var tcpSocket: ServerSocket? = null
    private var tcpConnectionHandler: TcpConnectionHandler? = null

    private var udpSocket: DatagramSocket? = null
    private var udpConnectionHandler: UdpConnectionHandler? = null

    private var port = 4444

    private var conn: Connection<*>? = null

    private val _logs = mutableListOf<LogItem>()
    val logs: MutableList<LogItem> = Collections.synchronizedList(_logs)

    init {
//        getIp()
    }

    fun getIp() {
        disposable.add(NetworkModule.ipService.getPublicIp()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    val ip = it.string().trim()
                    _ipState.value = ServerUiState.Loaded(ip)
                },
                onError = {
                    _ipState.value = ServerUiState.Error(it.message)
                }
            )
        )
    }

    private fun socketOpened(port: String) {
        _uiState.value = ServerButtonState.SocketOpened
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_opened, port))
    }

    private fun socketClosed(port: String) {
        _uiState.value = ServerButtonState.SocketClosed
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_closed, port))
    }

    private fun isValidPort(port: String): Boolean {
        return port.toIntOrNull() != null
    }

    fun onCloseSocketClicked() {
        closeTcpSocket()
        closeUdpSocket()

        socketClosed(port.toString())
    }

    private fun getPingHandler(socket: Socket): ServerTcpPingHandler {
        return ServerTcpPingHandler(socket) {
            handlePing(it)
        }
    }

    private fun handlePing(callback: ServerPingHandlerCallback) {
        when (callback) {
            is ServerPingHandlerCallback.PingGet -> {
                _singleEvent.postValue(SingleEvent.ConnEvent.PingGet(callback.ping))
            }
            ServerPingHandlerCallback.ConnectionClose -> {
                onCloseConnection()
            }
            ServerPingHandlerCallback.Timeout -> {
                addLog("Timeout")
            }
        }
    }

    private fun onCloseConnection() {
        when (conn) {
            is UdpConnection -> {
                ioViewModelScope.launch {
                    conn?.handler?.interrupt()
                    conn?.handler?.join()
                    conn = null

                    udpSocket?.disconnect()

                    udpConnectionHandler = getAndStartUdpConnectionHandler()
                }
            }
            is TcpConnection -> {
                ioViewModelScope.launch {
                    conn?.close()

                    conn = null

                    tcpConnectionHandler = getTcpConnectionHandler()
                }
            }
        }

        println("fefe on close")

        _connItem.postValue(null)
    }

    fun onStartTestClick(pos: Int) {
//        conn?.handler?.interrupt()
//        conn?.handler = null

//        val testHandler = conn.socket
//            ?.let { socket ->
//                getTestHandler(
//                    socket as Socket,
//                    onPacketReceived = { packet ->
//                        (packet as? PacketData)?.let {
//                            val ping = Calendar.getInstance().timeInMillis - it.time
//
//                            updateConnectionItem(pos, connectionItems[pos].copyStartTest())
//                            addLog(
//                                pos,
//                                "Packet received. Ping $ping. Data size ${it.dataSize}"
//                            )
//                        }
//                    },
//                    onUnknownPacketType = {
//                        addLog(pos, "Unknown packet", LogType.ERROR)
//                    },
//                    onClose = {
//                        updateConnectionItem(pos, connectionItems[pos].copyStopTestWithResult())
//                        addLog(pos, "Test end: client disconnect.")
//                    },
//                    onTestEnd = { testResult ->
//                        testResults[conn] = testResult
//
//                        updateConnectionItem(pos, connectionItems[pos].copyStopTestWithResult())
//                        addLog(pos, "Test end: success")
//                    }
//                )
//            }
//            ?: return
//
//        conn.handler = testHandler
//        conn.handler?.start()
    }

    private fun getTestHandler(
        socket: Socket,
        script: List<TestItem> = Scripts.testScript,
        onPacketReceived: (packet: Packet) -> Unit,
        onUnknownPacketType: (packetType: PacketType) -> Unit,
        onClose: () -> Unit,
        onTestEnd: (testResult: TestResult) -> Unit,
    ): TestHandler {
        return TestHandler(
            socket = socket,
            script = script,
            onPacketReceived = onPacketReceived,
            onUnknownPacketType = onUnknownPacketType,
            onClose = onClose,
            onTestEnd = onTestEnd
        )
    }

    private fun addLog(
        message: String,
        logType: LogType = LogType.MESSAGE
    ) {
        val log = LogItem(message = message, type = logType)
        _logs.add(log)
        _singleEvent.postValue(SingleEvent.ConnEvent.AddLog(_logs.lastIndex))
    }

    fun onStopTestClick(pos: Int) {
//        conn?.handler?.interrupt()
//        conn?.handler = null
//
//        (conn as? TcpConnection)?.let { conn ->
//            conn.handler = getPingHandler(conn.socket)
//            conn.handler?.start()
//        }
    }

    fun onResultClick(pos: Int) {
        TODO("Not yet implemented")
    }

    private fun openUdpSocket(port: String) {
        if (isValidPort(port)) {
            if (udpSocket == null) {
                udpSocket = DatagramSocket(port.toInt())

                _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_opened))

                udpConnectionHandler = getAndStartUdpConnectionHandler()

                this.port = port.toInt()
                socketOpened(port)
            } else {
                _singleEvent.value =
                    SingleEvent.ShowToastEvent(getString(R.string.socket_already_opened))
            }
        } else {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
        }
    }

    private fun getAndStartUdpConnectionHandler(): UdpConnectionHandler? {
        udpSocket?.let {
            return UdpConnectionHandler(
                it,
                onConnectAdd = {
                    onAddConnection(it)
                },
                onConnectFail = {

                }
            ).apply {
                start()
            }
        } ?: return null
    }

    private fun onAddConnection(dp: DatagramPacket) {
        ioViewModelScope.launch {
            udpSocket?.let { socket ->
                socket.connect(dp.socketAddress)

                udpConnectionHandler?.interrupt()
                udpConnectionHandler?.join()
                udpConnectionHandler = null

                conn = UdpConnection(
                    socket,
                    ServerUdpPingHandler(socket, this@ServerViewModel::handlePing).apply { start() }
                )

                _connItem.postValue(ConnectionItem.of(conn!!))
            }
        }
    }

    private fun onAddConnection(socket: Socket) {
        ioViewModelScope.launch {
            tcpConnectionHandler?.interrupt()
            tcpConnectionHandler?.join()
            tcpConnectionHandler = null

            conn = TcpConnection(
                socket,
                ServerTcpPingHandler(socket, this@ServerViewModel::handlePing).apply { start() })

            _connItem.postValue(ConnectionItem.of(conn!!))
        }
    }

    // TODO --- TCP ---

    private fun getTcpConnectionHandler(): TcpConnectionHandler? {
        tcpSocket?.let { socket ->
            return TcpConnectionHandler(
                socket,
                onConnectionAdd = { socket ->
                    onAddConnection(socket)
                },
                onClose = {
//                    onCloseConnection()
                }
            ).apply {
                start()
            }
        } ?: return null
    }

    private fun openTcpSocket(port: String) {
        if (isValidPort(port)) {
            if (tcpSocket == null) {
                try {
                    tcpSocket = ServerSocket(port.toInt())

                    tcpConnectionHandler = getTcpConnectionHandler()

                    this.port = port.toInt()
                    socketOpened(port)
                } catch (e: BindException) {
                    _singleEvent.value =
                        SingleEvent.ShowToastEvent(getString(R.string.socket_already_opened))
                    tcpSocket?.close()
                }
            } else {
                _singleEvent.value =
                    SingleEvent.ShowToastEvent(getString(R.string.socket_already_opened))
            }
        } else {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
        }
    }

    private fun closeTcpSocket() {
        tcpSocket?.let {
            ioViewModelScope.launch {
                conn?.close()

                tcpSocket!!.close()


                tcpSocket = null
            }

            _uiState.postValue(ServerButtonState.SocketClosed)
        }
    }

    private fun closeUdpSocket() {
        udpSocket?.let {
            ioViewModelScope.launch {
                if (udpSocket?.isConnected == true) {
                    udpSocket?.send(datagramPacketFromArray(UdpPacketDisconnect().send()))
                }

                conn?.close()

                udpSocket?.close()
                udpSocket = null
            }

            _uiState.postValue(ServerButtonState.SocketClosed)
        }
    }

    override fun onCleared() {
        closeTcpSocket()
        closeUdpSocket()
        super.onCleared()
    }

    fun onOpenSocketClicked(port: String, protocol: Protocol) {
        when (protocol) {
            Protocol.UDP -> openUdpSocket(port)
            Protocol.TCP -> openTcpSocket(port)
        }
    }

    companion object {
        private val TAG = ServerViewModel::class.java.simpleName
    }
}