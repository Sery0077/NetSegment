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
import sery.vlasenko.netsegment.domain.socket_handlers.ConnectionHandler
import sery.vlasenko.netsegment.domain.socket_handlers.PingHandler
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.service.MyHandler
import sery.vlasenko.netsegment.utils.ResourceProvider
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class ServerViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    private val _recyclerState: MutableSharedFlow<RecyclerState> = MutableSharedFlow(extraBufferCapacity = 20)
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
        getIp()
    }

    fun getIp() {
        disposable.add(NetworkModule.ipService.getPublicIp()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
//                addMessageToLogs(getString(R.string.ip_getting))
            }
            .subscribeBy(
                onSuccess = {
                    val ip = it.string().trim()
                    _ipState.value = ServerUiState.Loaded(ip)
//                    addMessageToLogs("${getString(R.string.ip_getted)} $ip")
                },
                onError = {
                    _ipState.value = ServerUiState.Error(it.message)
//                    addMessageToLogs("${getString(R.string.ip_getting_error)} ${it.message}")
                }
            )
        )
    }

    fun socketOpened(port: String) {
        _uiState.value = UiState.SocketOpened
//        addMessageToLogs(ResourceProvider.getString(R.string.socket_opened, port))
    }

    fun socketClosed(port: String) {
        _uiState.value = UiState.SocketClosed
//        addMessageToLogs(ResourceProvider.getString(R.string.socket_closed, port))
    }

    fun isValidPort(port: String): Boolean {
        return port.toIntOrNull() != null
    }

//    private fun addMessageToLogs(msg: String) {
//        val rawTime = Calendar.getInstance().timeInMillis
//
//        val time = rawTime.toTimeFormat()
//
////        logs.add(LogItem(time, msg))
//
//        viewModelScope.launch {
////            _recyclerState.emit(RecyclerState.LogAdd(logs.lastIndex))
//        }
//    }

    fun onCloseSocketClicked() {
        closeSocket()

        socketClosed(mPort.toString())
    }

    fun onOpenSocketClicked(port: String) {
        if (isValidPort(port)) {

            if (socket == null) {
                socket = ServerSocket(port.toInt())

                connectionHandler = ConnectionHandler(socket,
                    onConnectionAdd = {socket ->
                        onConnectionAdd(socket)
                        println("fefe add")
                    },
                    onClose = {
                        clearConnections()
                    }
                )
                connectionHandler?.start()

                socketOpened(port)
            } else {
//                addMessageToLogs(getString(R.string.socket_already_opened))
            }

        } else {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
        }
    }

    private fun onConnectionAdd(socket: Socket) {
        val index = connections.lastIndex + 1

        val conn = TcpConnection(socket, null)

        val handler = PingHandler(socket,
            onPingGet = {
                connections[index].ping = it
//                        connections[index].logs.add(LogItem(message = "Ping get"))
                _recyclerState.onNext(RecyclerState.ConnChanged(index, it))
//                        _recyclerState.onNext(RecyclerState.LogAdd(index, LogItem(message = "Ping get")))
            },
            onUnknownPacketType = {
                conn.handler?.interrupt()
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

        conn.handler = handler
        connections.add(conn)
        conn.handler?.start()

        _recyclerState.onNext(RecyclerState.ConnAdd(connections.lastIndex))
    }

    private fun closeSocket() {
        connectionHandler?.interrupt()
        socket?.close()

        connectionHandler = null
        socket = null
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

        println("StartTestClick" + Thread.currentThread().id)

        conn.handler?.interrupt()

        if (conn is TcpConnection) {
            val my = MyHandler(conn.socket, conn.input, conn.output) {}

            my.start()

            Thread {
                conn.handler = PingHandler(conn.socket, onClose = {}, onPingGet = {})
                conn.handler?.start()
            }.start()
        }
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