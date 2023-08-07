package sery.vlasenko.netsegment.ui.server

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.IOException
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
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem
import sery.vlasenko.netsegment.utils.ResourceProvider
import sery.vlasenko.netsegment.utils.TestScripts
import sery.vlasenko.netsegment.utils.toTimeFormat
import java.io.File
import java.io.OutputStreamWriter
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
        get() = _connItem

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

    private var port = -1

    private var conn: Connection<*>? = null

    private val _logs = mutableListOf<LogItem>()
    val logs: MutableList<LogItem> = Collections.synchronizedList(_logs)

    private var testResult: TestResult? = null

    init {
        getIp()
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
                    conn?.interruptHandler()
                    conn = null

                    udpSocket?.let { udpSocket ->
                        udpSocket.disconnect()
                        startListenUdpConnection(udpSocket)
                    }
                }
            }
            is TcpConnection -> {
                ioViewModelScope.launch {
                    conn?.close()
                    conn = null

                    tcpSocket?.let { tcpSocket ->
                        startListenTcpConnection(tcpSocket)
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
                _connItem.postValue(connItem.value?.copyResultAvailable())

                testResult =
                    if (testResult == null) callback.result else testResult?.append(callback.result)

                _connItem.postValue(_connItem.value?.copyStopTestWithResult())
                addLog(getString(R.string.measures_end))
                startPing()
            }
            ServerTestCallback.MeasuresStart -> {
                _connItem.postValue(_connItem.value?.copyStartTest())
                addLog(getString(R.string.measures_start))
            }
            ServerTestCallback.MeasuresStartFailed -> {
                _connItem.postValue(_connItem.value?.copyStopTest())
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
            testScript = TestScripts.testScript,
            iterationCount = iterationCount,
            callback = this::handleTestCallback
        )

    private fun getUdpTestHandler(
        connection: UdpConnection,
        iterationCount: Int
    ): ServerUdpMeasuresHandler =
        ServerUdpMeasuresHandler(
            socket = connection.socket,
            testScript = TestScripts.testScript,
            iterationCount = iterationCount,
            callback = this::handleTestCallback
        )

    private fun startTcpTest(connection: TcpConnection, iterationCount: Int) {
        ioViewModelScope.launch {
            connection.setAndStartNewHandler(getTcpTestHandler(connection, iterationCount))
        }
    }

    private fun startUdpTest(connection: UdpConnection, iterationCount: Int) {
        ioViewModelScope.launch {
            connection.setAndStartNewHandler(getUdpTestHandler(connection, iterationCount))
        }
    }

    fun onStartTestClick(iterationCount: Int) {
        conn?.let { conn ->
            _connItem.postValue(_connItem.value?.copyStartTest())
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
        conn?.interruptHandler()

        (conn as? TcpConnection)?.let { conn ->
            conn.setAndStartNewHandler(getTcpPingHandler(conn.socket))
        }

        (conn as? UdpConnection)?.let { conn ->
            conn.setAndStartNewHandler(getUdpPingHandler(conn.socket))
        }
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
            .also {
                startListenUdpConnection(it)
            }


        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_opened))

        this.port = port.toInt()
        socketOpened(port)
    }

    private fun startListenUdpConnection(udpSocket: DatagramSocket) {
        udpConnectionHandler = getUdpConnectionHandler(udpSocket).apply { start() }
    }

    private fun startListenTcpConnection(tcpSocket: ServerSocket) {
        tcpConnectionHandler = getTcpConnectionHandler(tcpSocket).apply { start() }
    }

    private fun getUdpConnectionHandler(socket: DatagramSocket): ServerUdpConnectionHandler {
        return ServerUdpConnectionHandler(
            socket = socket,
            onConnectAdd = this::onAddConnection
        )
    }

    private fun getUdpPingHandler(socket: DatagramSocket): ServerUdpPingHandler =
        ServerUdpPingHandler(
            socket = socket,
            callback = this::handlePing
        )

    private fun getTcpPingHandler(socket: Socket): ServerTcpPingHandler =
        ServerTcpPingHandler(
            socket = socket,
            callback = this::handlePing
        )

    private fun onAddConnection(dp: DatagramPacket) {
        ioViewModelScope.launch {
            udpSocket?.let { socket ->
                socket.connect(dp.socketAddress)

                udpConnectionHandler?.interrupt()
                udpConnectionHandler?.join()
                udpConnectionHandler = null

                conn = UdpConnection(
                    socket,
                    getUdpPingHandler(socket).apply { start() }
                ).also {
                    _connItem.postValue(ConnectionItem.of(it))
                }
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
                getTcpPingHandler(socket).apply { start() }
            ).also {
                _connItem.postValue(ConnectionItem.of(it))
            }
        }
    }

    private fun startPing() {
        ioViewModelScope.launch {
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
        conn.setAndStartNewHandler(getUdpPingHandler(conn.socket))
    }

    private fun startTcpPing(conn: TcpConnection) {
        conn.setAndStartNewHandler(getTcpPingHandler(conn.socket))
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

        try {
            tcpSocket = ServerSocket(port.toInt()).also { tcpSocket ->
                startListenTcpConnection(tcpSocket)
            }

            this.port = port.toInt()
            socketOpened(port)
        } catch (e: Exception) {
            _singleEvent.value = SingleEvent.ShowToastEvent(
                "${getString(R.string.open_socket_failed, port.toInt())} ${e.message}"
            )
        }
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

    fun onSaveResultClicked(context: Context) {
        ioViewModelScope.launch {
            testResult?.let {
                try {
                    val path = context.getExternalFilesDir(null)
                    val file =
                        File(path, "measure_${System.currentTimeMillis().toTimeFormat()}.csv")

                    val writer = OutputStreamWriter(file.outputStream(), Charsets.UTF_8)

                    writeDataSize(it, writer)
                    writeDelays(it, writer)
                    writeDelayJitter(it, writer)
                    writeLosses(it, writer)

                    writeAll(it, writer)

                    writer.close()

                    _singleEvent.postValue(SingleEvent.ShowToastEvent("Results is saved"))
                } catch (e: IOException) {
                    e.printStackTrace()
                    _singleEvent.postValue(SingleEvent.ShowToastEvent("${e.message}"))
                }
            }
        }
    }

    private fun writeAll(testResult: TestResult, writer: OutputStreamWriter) {
        writer.write("\n\n\n")

        writer.write("Sent packets size;")

        testResult.sentPacketsBySize.forEach {
            writer.write("${it.key};")
        }

        writer.write("\n")

        writer.write("Received packets size;")

        testResult.receivedPacketsBySize.forEach {
            writer.write("${it.key};")
        }

        writer.write("\n")

        testResult.delaysBySize.forEach { map ->
            writer.write("${map.key};")
            map.value.forEach {
                writer.write("$it;")
            }
            writer.write("\n")
        }

        writer.flush()
    }

    private fun writeLosses(testResult: TestResult, writer: OutputStreamWriter) {
        writer.write("\"Losses\";")

        testResult.sentPacketsBySize.forEach {
            val losses = testResult.receivedPacketsBySize.getOrElse(it.key) { -1 } - it.value
            writer.write("$losses;")
        }

        writer.write("\n")
        writer.flush()
    }

    private fun writeDelayJitter(testResult: TestResult, writer: OutputStreamWriter) {
        writer.write("\"Delay jitter, mcs\";")

        testResult.delaysBySize.forEach {
            val jitter = it.value.max() - it.value.min()
            writer.write("$jitter;")
        }

        writer.write("\n")
        writer.flush()
    }

    private fun writeDelays(testResult: TestResult, writer: OutputStreamWriter) {
        writer.write("\"Delays, mcs\";")

        testResult.averageDelaysBySize.values.forEach {
            writer.write("${it.toInt()};")
        }

        writer.write("\n")
        writer.flush()
    }

    private fun writeDataSize(testResult: TestResult, writer: OutputStreamWriter) {
        writer.write("\"Packet size, bytes\";")

        testResult.sentPacketsBySize.keys.forEach {
            writer.write("$it;")
        }

        writer.write("\n")
        writer.flush()
    }

    companion object {
        private val TAG = ServerViewModel::class.java.simpleName
    }
}