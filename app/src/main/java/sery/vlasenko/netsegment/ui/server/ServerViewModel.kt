package sery.vlasenko.netsegment.ui.server

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.data.NetworkModule
import sery.vlasenko.netsegment.domain.socket_handlers.server.*
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.model.connections.UdpConnection
import sery.vlasenko.netsegment.model.test.udp.UdpPacketDisconnect
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem
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

        _connItem.postValue(null)
    }

    private fun handleTestCallback(callback: ServerTestCallback) {
        when (callback) {
            is ServerTestCallback.MeasuresEnd -> {
                addLog("Test end")
                startPing()
            }
            ServerTestCallback.MeasuresStart -> {
                addLog("Test start")
            }
            ServerTestCallback.MeasuresStartFailed -> {
                addLog("Client don't start test")
                startPing()
            }
            ServerTestCallback.SocketClose -> {
                onCloseConnection()
            }
            is ServerTestCallback.PingGet -> {
                _singleEvent.postValue(SingleEvent.ConnEvent.PingGet(callback.ping))
            }
        }
    }

    private fun getTcpTestHandler(connection: TcpConnection): TcpMeasuresHandler =
        TcpMeasuresHandler(
            socket = connection.socket,
            testScript = Scripts.testScript,
            callback = this::handleTestCallback
        )

    private fun getUdpTestHandler(connection: UdpConnection): UdpMeasuresHandler =
        UdpMeasuresHandler(
            socket = connection.socket,
            callbackHandler = this::handleTestCallback
        )

    private fun startTcpTest(connection: TcpConnection) {
        connection.handler?.interrupt()
        connection.handler?.join()

        connection.handler = null

        connection.handler = getTcpTestHandler(connection).apply {
            start()
        }
    }

    private fun startUdpTest(connection: UdpConnection) {
        connection.handler?.interrupt()
        connection.handler?.join()

        connection.handler = null

        connection.handler = getUdpTestHandler(connection).apply {
            start()
        }
    }

    fun onStartTestClick() {
        ioViewModelScope.launch {
            conn?.let { conn ->
                when (conn) {
                    is TcpConnection -> {
                        startTcpTest(conn)
                    }
                    is UdpConnection -> {
                        startUdpTest(conn)
                    }
                }

            } ?: throw IllegalStateException("Failed to start test: connection is null")
        }
    }

    private fun addLog(
        message: String,
        logType: LogType = LogType.MESSAGE
    ) {
        val log = LogItem(message = message, type = logType)
        _logs.add(log)
        _singleEvent.postValue(SingleEvent.ConnEvent.AddLog(_logs.lastIndex))
    }

    fun onStopTestClick() {
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

    private fun startPing() {
        ioViewModelScope.launch {
            conn?.handler?.interrupt()
            conn?.handler?.join()

            (conn as? UdpConnection)?.let { conn ->
                conn.handler = ServerUdpPingHandler(
                    conn.socket,
                    callback = this@ServerViewModel::handlePing
                ).apply {
                    start()
                }
                return@launch
            }

            (conn as? TcpConnection)?.let { conn ->
                conn.handler = ServerTcpPingHandler(
                    conn.socket,
                    callback = this@ServerViewModel::handlePing
                ).apply {
                    start()
                }
            }
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