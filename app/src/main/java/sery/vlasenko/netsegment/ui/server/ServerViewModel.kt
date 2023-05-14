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
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.domain.socket_handlers.PingHandler
import sery.vlasenko.netsegment.ui.server.service.MyHandler
import sery.vlasenko.netsegment.utils.ResourceProvider
import sery.vlasenko.netsegment.utils.toTimeFormat
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ServerViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    private val _recyclerState: MutableSharedFlow<RecyclerState> = MutableSharedFlow()
    val recyclerState: SharedFlow<RecyclerState>
        get() = _recyclerState

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.SocketClosed)
    val uiState: LiveData<UiState>
        get() = _uiState

    private val _singleEvent: MutableLiveData<SingleEvent> = MutableLiveData()
    val singleEvent: LiveData<SingleEvent>
        get() = _singleEvent

    private var socket: ServerSocket? = null
    private var mPort = 4444

    private var isWorking = AtomicBoolean(false)

    val connections: MutableList<Connection<*>> = mutableListOf()
    val logs: MutableList<LogItem> = mutableListOf()

    private val socketListener = Thread {
        while (isWorking.get()) {
            val socket = socket?.accept()

            if (socket != null) {
                val index = connections.lastIndex + 1

                val conn = TcpConnection(socket, null)

                val handler = PingHandler(socket,
                    onPingGet = {
                        connections[index].ping = it
                        _recyclerState.onNext(RecyclerState.ConnChanged(index, it))

                        connections[index].logs.add(LogItem(message = "Ping get"))
                        _recyclerState.onNext(RecyclerState.LogAdd(index, LogItem(message = "Ping get")))
                    },
                    onUnknownPacketType = {
//                        _recyclerState.onNext(RecyclerState.LogAdd())
                    },
                    close = {
                        connections.removeAt(index)
                        _recyclerState.onNext(RecyclerState.ConnRemove(index))
                    }
                )

                conn.handler = handler
                connections.add(conn)
                conn.handler?.start()

                _recyclerState.onNext(RecyclerState.ConnAdd(connections.lastIndex))
            }
        }
    }.apply {
        isDaemon = true
    }

    init {
        getIp()
    }

    fun getIp() {
        disposable.add(NetworkModule.ipService.getPublicIp()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                addMessageToLogs(getString(R.string.ip_getting))
            }
            .subscribeBy(
                onSuccess = {
                    val ip = it.string().trim()
                    _ipState.value = ServerUiState.Loaded(ip)
                    addMessageToLogs("${getString(R.string.ip_getted)} $ip")
                },
                onError = {
                    _ipState.value = ServerUiState.Error(it.message)
                    addMessageToLogs("${getString(R.string.ip_getting_error)} ${it.message}")
                }
            )
        )
    }

    fun socketOpened(port: String) {
        _uiState.value = UiState.SocketOpened
        addMessageToLogs(ResourceProvider.getString(R.string.socket_opened, port))
    }

    fun socketClosed(port: String) {
        _uiState.value = UiState.SocketClosed
        addMessageToLogs(ResourceProvider.getString(R.string.socket_closed, port))
    }

    fun isValidPort(port: String): Boolean {
        return port.toIntOrNull() != null
    }

    private fun addMessageToLogs(msg: String) {
        val rawTime = Calendar.getInstance().timeInMillis

        val time = rawTime.toTimeFormat()

        logs.add(LogItem(time, msg))

        viewModelScope.launch {
//            _recyclerState.emit(RecyclerState.LogAdd(logs.lastIndex))
        }
    }

    fun onCloseSocketClicked() {
        closeSocket()

        socketClosed(mPort.toString())
    }

    fun onOpenSocketClicked(port: String) {
        if (isValidPort(port)) {

            if (socket == null) {
                socket = ServerSocket(port.toInt())

                socketListener.start()
                isWorking.set(true)

                socketOpened(port)
            } else {
                addMessageToLogs(getString(R.string.socket_already_opened))
            }

        } else {
            _singleEvent.value =
                SingleEvent.ShowToastEvent(ResourceProvider.getString(R.string.incorrect_port))
        }
    }

    private fun closeSocket() {
        socketListener.interrupt()

        connections.forEach {
            it.close()
        }

        isWorking.set(false)

        socket?.close()
        socket = null
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
                conn.handler = PingHandler(conn.socket, close = {}, onPingGet = {})
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