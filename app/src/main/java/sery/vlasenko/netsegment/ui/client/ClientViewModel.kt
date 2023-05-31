package sery.vlasenko.netsegment.ui.client

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.data.NetworkModule
import sery.vlasenko.netsegment.domain.socket_handlers.client.ClientTcpHandler
import sery.vlasenko.netsegment.domain.socket_handlers.client.ClientUdpHandler
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.model.connections.UdpConnection
import sery.vlasenko.netsegment.model.test.udp.UdpPacketConnect
import sery.vlasenko.netsegment.model.test.udp.UdpPacketDisconnect
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.ServerUiState
import sery.vlasenko.netsegment.ui.server.SingleEvent
import sery.vlasenko.netsegment.ui.server.log.LogState
import sery.vlasenko.netsegment.utils.Timeouts.CONNECTION_TIMEOUT
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import sery.vlasenko.netsegment.utils.datagramPacketFromSize
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

class ClientViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    private val _logs = mutableListOf<LogItem>()
    val logs: MutableList<LogItem> = Collections.synchronizedList(_logs)

    private val _logState: MutableSharedFlow<LogState> = MutableSharedFlow()
    val logState: SharedFlow<LogState>
        get() = _logState

    private val _uiState: MutableLiveData<ClientUiState> =
        MutableLiveData(ClientUiState.Disconnected)
    val uiState: LiveData<ClientUiState>
        get() = _uiState

    private val _singleEvent: MutableLiveData<SingleEvent> = MutableLiveData()
    val singleEvent: LiveData<SingleEvent>
        get() = _singleEvent

    private var conn: Connection<*>? = null

    private val disconnect = datagramPacketFromArray(UdpPacketDisconnect().send())

    private val connectionRetry = 3

    init {
        getIp()
    }

    fun getIp() {
        disposable.add(
            NetworkModule.ipService.getPublicIp()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    addMessageToLogs(getString(R.string.ip_getting))
                }
                .subscribeBy(
                    onSuccess = {
                        val ip = it.string().trim()
                        _ipState.value = ServerUiState.Loaded(ip)
                        addMessageToLogs("${getString(R.string.ip_got)} $ip")
                    },
                    onError = {
                        _ipState.value = ServerUiState.Error(it.message)
                        addMessageToLogs("${getString(R.string.ip_getting_error)} ${it.message}")
                    }
                )
        )
    }

    fun onConnectClicked(ip: String, port: String, protocol: Protocol) {
        if (protocol == Protocol.TCP) {
            connectTcpSocket(ip, port)
        } else {
            connectUdpSocket(ip, port)
        }
    }

    fun onDisconnectClicked() {
        closeSocket()
    }

    private fun connectTcpSocket(ip: String, port: String) {
        addMessageToLogs(getString(R.string.connect_try, ip, port))
        _uiState.value = ClientUiState.Connecting

        ioViewModelScope.launch {
            for (i in 1..connectionRetry) {
                try {
                    val socket = Socket().apply {
                        tcpNoDelay = true
                    }

                    socket.connect(
                        InetSocketAddress(ip, port.toInt()),
                        CONNECTION_TIMEOUT
                    )

                    if (socket.isConnected) {
                        socketConnected(socket, ip, port)
                        break
                    } else {
                        addMessageToLogs(
                            getString(R.string.connect_error, "$ip $port"),
                            LogType.ERROR
                        )
                    }
                } catch (e: Exception) {
                    addMessageToLogs(
                        getString(R.string.connect_error, ip, port),
                        LogType.ERROR
                    )
                    addMessageToLogs(e.message.toString(), LogType.ERROR)
                }
            }

            if (conn == null) {
                _uiState.postValue(ClientUiState.Disconnected)
            }
        }
    }

    private fun connectUdpSocket(ip: String, port: String) {
        addMessageToLogs(getString(R.string.connect_try, ip, port))
        _uiState.value = ClientUiState.Connecting

        ioViewModelScope.launch {
            for (i in 1..connectionRetry) {
                try {
                    val addr = InetSocketAddress(ip, port.toInt())
                    val socket = DatagramSocket().apply {
                        soTimeout = CONNECTION_TIMEOUT
                    }

                    socket.send(
                        datagramPacketFromArray(
                            UdpPacketConnect(isAnswer = false).send(),
                            addr
                        )
                    )

                    val answer = datagramPacketFromSize(UdpPacketConnect.packetSize)

                    socket.receive(answer)

                    if (answer.data[0].toInt() == 6) {
                        socket.connect(addr)

                        addMessageToLogs(
                            getString(
                                R.string.connected, ip, port
                            )
                        )
                        _uiState.postValue(ClientUiState.Connected)

                        conn = UdpConnection(
                            socket,
                            getUdpHandler(socket, this@ClientViewModel::handleCallback).apply {
                                start()
                            })
                        break
                    } else {
                        addMessageToLogs(
                            getString(R.string.connect_error, addr.hostName, addr.port.toString()),
                            LogType.ERROR
                        )
                    }
                } catch (e: Exception) {
                    addMessageToLogs(getString(R.string.connect_error, ip, port), LogType.ERROR)
                    addMessageToLogs(e.message.toString(), LogType.ERROR)
                }
            }

            if (conn == null) {
                _uiState.postValue(ClientUiState.Disconnected)
            }
        }
    }

    private fun socketConnected(socket: Any, ip: String, port: String) {
        addMessageToLogs(
            getString(
                R.string.connected, ip, port
            )
        )
        _uiState.postValue(ClientUiState.Connected)

        when (socket) {
            is Socket -> {
                conn = TcpConnection(
                    socket,
                    getTcpHandler(socket, this::handleCallback).apply {
                        start()
                    }
                )
            }
            is DatagramSocket -> {
                conn = UdpConnection(
                    socket,
                    getUdpHandler(socket, this::handleCallback).apply {
                        start()
                    }
                )
            }
        }
    }

    private fun getUdpHandler(
        socket: DatagramSocket,
        handleCallback: (ClientHandlerCallback) -> Unit
    ): ClientUdpHandler {
        return ClientUdpHandler(
            socket = socket,
            callback = handleCallback
        )
    }

    private fun handleCallback(callback: ClientHandlerCallback) {
        when (callback) {
            ClientHandlerCallback.MeasuresEnd -> {
                _singleEvent.postValue(SingleEvent.ConnState.ConnIdle)
                addMessageToLogs(getString(R.string.measures_end))
            }
            ClientHandlerCallback.MeasuresStart -> {
                _singleEvent.postValue(SingleEvent.ConnState.ConnMeasure)
                addMessageToLogs(getString(R.string.measures_start))
            }
            is ClientHandlerCallback.PingGet -> {
                _singleEvent.postValue(SingleEvent.ConnEvent.PingGet((callback.ping / 1000).toString()))
            }
            ClientHandlerCallback.SocketClose -> {
                onCloseSocketReceive()
            }
            ClientHandlerCallback.Timeout -> {
                _singleEvent.postValue(SingleEvent.ConnEvent.PingGet("--"))
                addMessageToLogs("Timeout")
            }
            is ClientHandlerCallback.Except -> {
                addMessageToLogs(callback.e.stackTraceToString())
            }
        }
    }

    private fun getTcpHandler(
        socket: Socket,
        callback: (ClientHandlerCallback) -> Unit
    ): ClientTcpHandler {
        return ClientTcpHandler(
            socket = socket,
            callback = {
                callback.invoke(it)
            }
        )
    }

    private fun onCloseSocketReceive() {
        val port = conn?.port ?: 0

        ioViewModelScope.launch {
            conn?.let {
                conn?.handler?.interrupt()
                conn?.handler?.join()

                conn?.socket?.close()

                conn = null
            }
        }

        addMessageToLogs(getString(R.string.socket_closed, port.toString()))
        _uiState.postValue(ClientUiState.Disconnected)
    }

    private fun closeSocket() {
        val port = conn?.port ?: 0

        ioViewModelScope.launch {
            conn?.let {
                conn?.handler?.interrupt()
                conn?.handler?.join()


                if ((conn?.socket as? DatagramSocket)?.isConnected == true) {
                    (conn?.socket as? DatagramSocket)?.send(disconnect)
                }

                conn?.socket?.close()

                conn = null
            }
        }

        addMessageToLogs(getString(R.string.socket_closed, port.toString()))
        _uiState.postValue(ClientUiState.Disconnected)
    }

    private fun addMessageToLogs(message: String, type: LogType = LogType.MESSAGE) {
        logs.add(LogItem(message = message, type = type))
        _logState.onNext(LogState.LogAdd(logs.lastIndex))
    }

    private fun <T> MutableSharedFlow<T>.onNext(value: T) {
        ioViewModelScope.launch {
            this@onNext.emit(value)
        }
    }

    override fun onCleared() {
        closeSocket()
        super.onCleared()
    }

}