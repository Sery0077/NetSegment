package sery.vlasenko.netsegment.ui.server

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.data.NetworkModule
import sery.vlasenko.netsegment.domain.socket_handlers.server.TcpConnectionHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.PingHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.TestHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.UdpConnectionHandler
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.PacketData
import sery.vlasenko.netsegment.model.test.TestResult
import sery.vlasenko.netsegment.model.testscripts.TestItem
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.ResourceProvider
import sery.vlasenko.netsegment.utils.Scripts
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class ServerViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    private val _connRecyclerState: MutableSharedFlow<ConnRecyclerState> =
        MutableSharedFlow(extraBufferCapacity = 20)
    val connRecyclerState: SharedFlow<ConnRecyclerState>
        get() = _connRecyclerState

    private val _uiState: MutableLiveData<ServerButtonState> = MutableLiveData()
    val uiState: LiveData<ServerButtonState>
        get() = _uiState

    private val _singleEvent: MutableLiveData<SingleEvent> = MutableLiveData()
    val singleEvent: LiveData<SingleEvent>
        get() = _singleEvent

    private var tcpSocket: ServerSocket? = null
    private var tcpHandler: TcpConnectionHandler? = null
    private var tcpPort = 4444

    private var udpSocket: DatagramSocket? = null
    private var udpHandler: UdpConnectionHandler? = null
    private var udpPort = 4445

    private val testResults: HashMap<Connection<*>, TestResult?> = hashMapOf()
    private val connections: MutableList<Connection<*>> = mutableListOf()

    val connectionItems =  object: MutableList<ConnectionItem> by ArrayList() {
        override fun add(element: ConnectionItem): Boolean {
            add(lastIndex + 1, element)
            _connRecyclerState.onNext(ConnRecyclerState.ConnAdd(lastIndex))
            return true
        }
    }

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

    private fun tcpSocketOpened(port: String) {
        _uiState.value = ServerButtonState.TcpSocketOpened
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_opened, port))
    }

    private fun tcpSocketClosed(port: String) {
        _uiState.value = ServerButtonState.TcpSocketClosed
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_closed, port))
    }

    private fun udpSocketOpened(port: String) {
        _uiState.value = ServerButtonState.UdpSocketOpened
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_opened, port))
    }

    private fun udpSocketClosed(port: String) {
        _uiState.value = ServerButtonState.UdpSocketClosed
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_closed, port))
    }

    private fun isValidPort(port: String): Boolean {
        return port.toIntOrNull() != null
    }

    fun onCloseTcpSocketClicked() {
        closeTcpSocket()

        tcpSocketClosed(tcpPort.toString())
    }

    fun onOpenTcpSocketClicked(port: String) {
        if (isValidPort(port)) {
            if (tcpSocket == null) {
                tcpSocket = ServerSocket(port.toInt())

                tcpHandler = TcpConnectionHandler(tcpSocket!!,
                    onConnectionAdd = { socket ->
                        onConnectionAdd(socket)
                    },
                    onClose = {
                        clearTcpConnections()
                    }
                )
                tcpHandler?.start()

                tcpSocketOpened(port)
            } else {
                _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_already_opened))
            }
        } else {
            _singleEvent.value = SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
        }
    }

    private fun onConnectionAdd(socket: Socket) {
        val conn = TcpConnection(socket, getPingHandler(socket)).apply {
            handler?.start()
        }
        connections.add(conn)
        testResults[conn] = null

        connectionItems.add(ConnectionItem.of(conn))
    }

    private fun getPingHandler(socket: Socket): PingHandler {
        val index = connections.lastIndex + 1

        return PingHandler(socket,
            onPingGet = { ping ->
                updateConnectionItem(index, connectionItems[index].copyPingUpdate(ping))
                addLogToConnectionItem(index, "Ping get")
            },
            onUnknownPacketType = {
//                conn.handler?.interrupt()
                _connRecyclerState.onNext(
                    ConnRecyclerState.LogAdd(
                        index,
                        LogItem(message = "Unknown packet $it", type = LogType.ERROR)
                    )
                )
            },
            onClose = {
                connections.removeAt(index)
                _connRecyclerState.onNext(ConnRecyclerState.ConnRemove(index))
            }
        )
    }

    private fun closeTcpSocket() {
        tcpHandler?.interrupt()
        tcpSocket?.close()

        tcpHandler = null
        tcpSocket = null

        clearTcpConnections()
    }

    private fun clearTcpConnections() {
        connections.forEachIndexed { index, connection ->
            if (connection.protocol == Protocol.TCP) {
                connection.handler?.interrupt()
                connection.close()
                connections.removeAt(index)
            }
        }

        clearTcpConnectionItems()
    }

    fun onStartTestClick(pos: Int) {
        val conn = connections[pos]
        conn.handler?.interrupt()
        conn.handler = null

        val newConnectionItem = connectionItems[pos].copyStartTest()
        updateConnectionItemWithLog(
            index = pos,
            message = "Test start",
            connectionItem = newConnectionItem
        )

        val testHandler = conn.socket
            ?.let { socket ->
                getTestHandler(
                    socket as Socket,
                    onPacketReceived = { packet ->
                        (packet as? PacketData)?.let {
                            val ping = Calendar.getInstance().timeInMillis - it.time

                            updateConnectionItem(pos, connectionItems[pos].copyStartTest())
                            addLogToConnectionItem(pos, "Packet received. Ping $ping. Data size ${it.dataSize}")
                        }
                    },
                    onUnknownPacketType = {
                        addLogToConnectionItem(pos, "Unknown packet", LogType.ERROR)
                    },
                    onClose = {
                        updateConnectionItem(pos, connectionItems[pos].copyStopTestWithResult())
                        addLogToConnectionItem(pos, "Test end: client disconnect.")
                    },
                    onTestEnd = { testResult ->
                        testResults[conn] = testResult

                        updateConnectionItem(pos, connectionItems[pos].copyStopTestWithResult())
                        addLogToConnectionItem(pos, "Test end: success")
                    }
                )
            }
            ?: return

        conn.handler = testHandler
        conn.handler?.start()
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

    private fun clearTcpConnectionItems() {
        connectionItems.forEachIndexed { index, conn ->
            if (conn.protocol == Protocol.TCP) {
                connectionItems.removeAt(index)
                _connRecyclerState.onNext(ConnRecyclerState.ConnRemove(index))
            }
        }
    }

    private fun updateConnectionItem(index: Int, connectionItem: ConnectionItem) {
        connectionItems[index] = connectionItem
        _connRecyclerState.onNext(ConnRecyclerState.ConnChanged(index, connectionItem))
    }

    private fun updateConnectionItemWithLog(index: Int, message: String, logType: LogType = LogType.MESSAGE, connectionItem: ConnectionItem) {
        connectionItems[index] = connectionItem
        connectionItems[index].logs.add(LogItem(message = message, type = logType))
        _connRecyclerState.onNext(ConnRecyclerState.ConnChanged(index, connectionItem))
    }

    private fun addLogToConnectionItem(index: Int, message: String, logType: LogType = LogType.MESSAGE) {
        val log = LogItem(message = message, type = logType)
        connectionItems[index].logs.add(log)
        _connRecyclerState.onNext(ConnRecyclerState.LogAdd(index, log))
    }

    fun onStopTestClick(pos: Int) {
        val conn = connections[pos]

        if (conn is TcpConnection) {
            with(conn) {
                handler?.interrupt()
                handler = null

                handler = getPingHandler(conn.socket)
                handler?.start()
            }
        } else {
            // TODO add UdpConnection
        }
    }

    fun onResultClick(pos: Int) {
        TODO("Not yet implemented")
    }

    private fun <T> MutableSharedFlow<T>.onNext(value: T) {
        viewModelScope.launch {
            this@onNext.emit(value)
        }
    }

    fun onOpenUdpSocketClicked(port: String) {
        if (isValidPort(port)) {
            if (udpSocket == null) {
                udpSocket = DatagramSocket(port.toInt())

                udpHandler = UdpConnectionHandler(
                    udpSocket!!,
                    onConnectSuccess = {

                    },
                    onConnectFail = {

                    }
                )
                udpHandler?.start()

                udpSocketOpened(port)
            } else {
                _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_already_opened))
            }
        } else {
            _singleEvent.value = SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
        }
    }

    fun onCloseUdpSocketClicked() {
        closeUdpSocket()

        udpSocketClosed(tcpPort.toString())
    }

    private fun closeUdpSocket() {
        udpSocket?.close()
        udpSocket = null

        clearUdpConnections()
    }

    private fun clearUdpConnections() {
        connections.forEachIndexed { index, connection ->
            if (connection.protocol == Protocol.UDP) {
                connection.handler?.interrupt()
                connection.close()
                connections.removeAt(index)
            }
        }

        clearUdpConnectionItems()
    }

    private fun clearUdpConnectionItems() {
        connectionItems.forEachIndexed { index, conn ->
            if (conn.protocol == Protocol.UDP) {
                connectionItems.removeAt(index)
                _connRecyclerState.onNext(ConnRecyclerState.ConnRemove(index))
            }
        }
    }

    override fun onCleared() {
        closeTcpSocket()
        closeUdpSocket()
        super.onCleared()
    }

    companion object {
        private val TAG = ServerViewModel::class.java.simpleName
    }
}