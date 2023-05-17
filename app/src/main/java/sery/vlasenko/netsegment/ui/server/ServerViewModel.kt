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
import sery.vlasenko.netsegment.domain.socket_handlers.server.ConnectionHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.PingHandler
import sery.vlasenko.netsegment.domain.socket_handlers.server.TestHandler
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.PacketData
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.service.MyHandler
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.ResourceProvider
import java.net.ServerSocket
import java.net.Socket
import java.util.Calendar

class ServerViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    private val _recyclerState: MutableSharedFlow<RecyclerState> =
        MutableSharedFlow(extraBufferCapacity = 20)
    val recyclerState: SharedFlow<RecyclerState>
        get() = _recyclerState

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.SocketClosed)
    val uiState: LiveData<UiState>
        get() = _uiState

    private val _singleEvent: MutableLiveData<SingleEvent> = MutableLiveData()
    val singleEvent: LiveData<SingleEvent>
        get() = _singleEvent

    private var socket: ServerSocket? = null
    private var connectionHandler: ConnectionHandler? = null

    private var mPort = 4444

    val connections: MutableList<Connection<*>> = mutableListOf()

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

    fun socketOpened(port: String) {
        _uiState.value = UiState.SocketOpened
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_opened, port))
    }

    fun socketClosed(port: String) {
        _uiState.value = UiState.SocketClosed
        _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_closed, port))
    }

    fun isValidPort(port: String): Boolean {
        return port.toIntOrNull() != null
    }

    fun onCloseSocketClicked() {
        closeSocket()

        socketClosed(mPort.toString())
    }

    fun onOpenSocketClicked(port: String) {
        if (isValidPort(port)) {
            if (socket == null) {
                socket = ServerSocket(port.toInt())

                connectionHandler = ConnectionHandler(socket,
                    onConnectionAdd = { socket ->
                        onConnectionAdd(socket)
                    },
                    onClose = {
                        clearConnections()
                    }
                )
                connectionHandler?.start()

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

    private fun onConnectionAdd(socket: Socket) {
        val conn = TcpConnection(socket, getPingHandler(socket)).apply {
            handler?.start()
        }
        connections.add(conn)

        _recyclerState.onNext(RecyclerState.ConnAdd(connections.lastIndex))
    }

    private fun getPingHandler(socket: Socket): PingHandler {
        val index = connections.lastIndex + 1

        return PingHandler(socket,
            onPingGet = {
                connections[index].ping = it
                connections[index].logs.add(LogItem(message = "Ping get"))
                _recyclerState.onNext(RecyclerState.ConnChanged(index, it))
                _recyclerState.onNext(RecyclerState.LogAdd(index, LogItem(message = "Ping get")))
            },
            onUnknownPacketType = {
//                conn.handler?.interrupt()
                _recyclerState.onNext(
                    RecyclerState.LogAdd(
                        index,
                        LogItem(message = "Unknown packet $it", type = LogType.ERROR)
                    )
                )
            },
            onClose = {
                connections.removeAt(index)
                _recyclerState.onNext(RecyclerState.ConnRemove(index))
            }
        )
    }

    private fun closeSocket() {
        connectionHandler?.interrupt()
        socket?.close()

        connectionHandler = null
        socket = null

        clearConnections()
    }

    private fun clearConnections() {
        connections.forEachIndexed { index, connection ->
            connection.handler?.interrupt()
            connection.close()

            _recyclerState.onNext(RecyclerState.ConnRemove(index))
        }
        connections.clear()
    }

    override fun onCleared() {
        closeSocket()
        super.onCleared()
    }

    fun onStartTestClick(pos: Int) {
        val conn = connections[pos]
        conn.handler?.interrupt()
        conn.handler = null

        val testHandler = conn.socket
            ?.let { socket ->
                getTestHandler(
                    socket as Socket,
                    onPacketReceived = { packet ->
                        (packet as? PacketData)?.let {
                            conn.ping = Calendar.getInstance().timeInMillis - it.time
                            conn.logs.add(LogItem(message = "Ping get ${it.dataSize}"))
                            _recyclerState.onNext(RecyclerState.ConnChanged(pos, conn.ping))
                            _recyclerState.onNext(RecyclerState.LogAdd(pos, LogItem(message = "Ping get")))
                        }
                    },
                    onUnknownPacketType = {

                    },
                    onClose = {

                    }
                )
            }
            ?: return

        conn.handler = testHandler
        conn.handler?.start()
    }

    private fun getTestHandler(
        socket: Socket,
        onPacketReceived: (packet: Packet) -> Unit,
        onUnknownPacketType: (packetType: PacketType) -> Unit,
        onClose: () -> Unit,
    ): TestHandler {
        return TestHandler(
            socket = socket,
            onPacketReceived = onPacketReceived,
            onUnknownPacketType = onUnknownPacketType,
            onClose = onClose
        )
    }

    private fun <T> MutableSharedFlow<T>.onNext(value: T) {
        viewModelScope.launch {
            this@onNext.emit(value)
        }
    }

    companion object {
        private val TAG = ServerViewModel::class.java.simpleName
    }
}