package sery.vlasenko.netsegment.ui.server

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.data.NetworkModule
import sery.vlasenko.netsegment.domain.socket_handlers.server.tcp.ServerTcpConnectionHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.tcp.ServerTcpMeasuresHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.tcp.ServerTcpPingHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.udp.ServerUdpConnectionHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.udp.ServerUdpMeasuresHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.udp.ServerUdpPingHandler
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.model.connections.UdpConnection
import sery.vlasenko.netsegment.model.test.TestResult
import sery.vlasenko.netsegment.model.test.udp.UdpPacketDisconnect
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem
import sery.vlasenko.netsegment.utils.ResourceProvider
import sery.vlasenko.netsegment.utils.Scripts
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
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
    private var tcpConnectionHandler: ServerTcpConnectionHandler? = null

    private var udpSocket: DatagramSocket? = null
    private var udpConnectionHandler: ServerUdpConnectionHandler? = null

    private var port = 4444

    private var conn: Connection<*>? = null

    private val _logs = mutableListOf<LogItem>()
    val logs: MutableList<LogItem> = Collections.synchronizedList(_logs)

    private var testResult: TestResult? = null

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
                _singleEvent.postValue(SingleEvent.ConnEvent.PingGet((callback.ping / 1000).toString()))
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
                    conn?.close()
                    conn = null

                    udpSocket?.let { udpSocket ->
                        udpSocket.disconnect()
                        udpConnectionHandler = getUdpConnectionHandler(udpSocket).apply { start() }
                    }
                }
            }
            is TcpConnection -> {
                ioViewModelScope.launch {
                    conn?.close()
                    conn = null

                    tcpSocket?.let { serverSocket ->
                        tcpConnectionHandler =
                            getTcpConnectionHandler(serverSocket).apply { start() }
                    }
                }
            }
        }

        _connItem.postValue(null)
        _logs.clear()
        testResult = null
    }

    private fun handleTestCallback(callback: ServerTestCallback) {
        when (callback) {
            is ServerTestCallback.MeasuresEnd -> {
                _connItem.postValue(connItem.value!!.copyResultAvailable())

                testResult =
                    if (testResult == null) callback.result else testResult?.append(callback.result)

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
                _singleEvent.postValue(SingleEvent.ConnEvent.PingGet((callback.ping / 1000).toString()))
            }
        }
    }

    private fun getTcpTestHandler(
        connection: TcpConnection,
        iterationCount: Int
    ): ServerTcpMeasuresHandler =
        ServerTcpMeasuresHandler(
            socket = connection.socket,
            testScript = Scripts.testScript,
            iterationCount = iterationCount,
            callback = this::handleTestCallback
        )

    private fun getUdpTestHandler(
        connection: UdpConnection,
        iterationCount: Int
    ): ServerUdpMeasuresHandler =
        ServerUdpMeasuresHandler(
            socket = connection.socket,
            testScript = Scripts.testScript,
            iterationCount = iterationCount,
            callback = this::handleTestCallback
        )

    private fun startTcpTest(connection: TcpConnection, iterationCount: Int) {
        ioViewModelScope.launch {
            connection.handler?.interrupt()
            connection.handler?.join()

            connection.handler = null

            connection.handler = getTcpTestHandler(connection, iterationCount).apply {
                start()
            }
        }
    }

    private fun startUdpTest(connection: UdpConnection, iterationCount: Int) {
        ioViewModelScope.launch {
            connection.handler?.interrupt()
            connection.handler?.join()

            connection.handler = null

            connection.handler = getUdpTestHandler(connection, iterationCount).apply {
                start()
            }
        }
    }

    fun onStartTestClick(iterationCount: Int) {
        conn?.let { conn ->
                when (conn) {
                    is TcpConnection -> {
                        startTcpTest(conn, iterationCount)
                    }
                    is UdpConnection -> {
                        startUdpTest(conn, iterationCount)
                    }
                }
            }
            ?: throw IllegalStateException("Failed to start test: connection is null")
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

    fun onResultClick() {
        println(testResult)
    }

    private fun openUdpSocket(port: String) {
        if (!isValidPort(port)) {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
            return
        }

        if (udpSocket != null) {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(getString(R.string.socket_already_opened))
            return
        }

        udpSocket = DatagramSocket(port.toInt())
        udpConnectionHandler = getUdpConnectionHandler(udpSocket!!).apply { }

        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_opened))

        this.port = port.toInt()
        socketOpened(port)
    }

    private fun getUdpConnectionHandler(socket: DatagramSocket): ServerUdpConnectionHandler {
        return ServerUdpConnectionHandler(
            socket = socket,
            onConnectAdd = this::onAddConnection
        )
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
            tcpConnectionHandler = null

            conn = TcpConnection(
                socket,
                ServerTcpPingHandler(socket, this@ServerViewModel::handlePing).apply { start() })

            _connItem.postValue(ConnectionItem.of(conn!!))
        }
    }

    private fun startPing() {
        ioViewModelScope.launch {
            conn?.handler?.run {
                interrupt()
                join()
            }

            (conn as? UdpConnection)?.let { conn ->
                startUdpPing(conn)
                return@launch
            }

            (conn as? TcpConnection)?.let { conn ->
                startTcpPing(conn)
            }
        }
    }

    private fun startUdpPing(conn: UdpConnection) {
        conn.handler = ServerUdpPingHandler(
            socket = conn.socket,
            callback = this::handlePing
        ).apply {
            start()
        }
    }

    private fun startTcpPing(conn: TcpConnection) {
        conn.handler = ServerTcpPingHandler(
            socket = conn.socket,
            callback = this::handlePing
        ).apply {
            start()
        }
    }

    // TODO --- TCP ---

    private fun getTcpConnectionHandler(socket: ServerSocket): ServerTcpConnectionHandler {
        return ServerTcpConnectionHandler(
            socket = socket,
            onConnectionAdd = { acceptedSocket ->
                onAddConnection(acceptedSocket)
            }
        )
    }

    private fun openTcpSocket(port: String) {
        if (!isValidPort(port)) {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
            return
        }

        if (tcpSocket != null) {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(getString(R.string.socket_already_opened))
            return
        }

        tcpSocket = ServerSocket(port.toInt()).also {
            getTcpConnectionHandler(it).apply { start() }
        }

        this.port = port.toInt()
        socketOpened(port)
    }

    private fun closeTcpSocket() {
        ioViewModelScope.launch {
            conn?.close()

            tcpConnectionHandler?.interrupt()
            tcpSocket?.close()

            tcpSocket = null
        }

        _uiState.postValue(ServerButtonState.SocketClosed)
    }

    private fun closeUdpSocket() {
        ioViewModelScope.launch {
            if (udpSocket?.isConnected == true) {
                udpSocket?.send(datagramPacketFromArray(UdpPacketDisconnect().send()))
            }

            conn?.close()

            udpConnectionHandler?.interrupt()
            udpSocket?.close()

            udpSocket = null
        }

        _uiState.postValue(ServerButtonState.SocketClosed)
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