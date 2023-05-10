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
import sery.vlasenko.netsegment.ui.server.service.TcpClientHandler
import sery.vlasenko.netsegment.ui.server.service.TcpHandler
import sery.vlasenko.netsegment.utils.ResourceProvider
import sery.vlasenko.netsegment.utils.toTimeFormat
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
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

//    private val runnable = Runnable {
//        var socket: Socket? = null
//        try {
//            this.socket = ServerSocket(mPort)
//
//            while (isWorking.get()) {
//                if (this.socket != null) {
//                    socket = this.socket!!.accept()
//
//                    val dataInputStream = DataInputStream(socket.getInputStream())
//                    val dataOutputStream = DataOutputStream(socket.getOutputStream())
//
//                    val t: Thread = TcpClientHandler(dataInputStream, dataOutputStream)
//                    t.start()
//                }
//            }
//
//        } catch (e: IOException) {
//            Log.e(TAG, e.stackTraceToString())
//            try {
//                socket?.close()
//            } catch (ex: IOException) {
//                Log.e(TAG, ex.stackTraceToString())
//            }
//        }
//    }

    private val socketListener = Thread {
        while (isWorking.get()) {
            val socket = socket?.accept()

            if (socket != null) {
                val conn = TcpConnection(socket)

                connections.add(conn)

                val index = connections.lastIndex

                println("socket" + socket.inetAddress.hostAddress)

                TcpHandler(
                    BufferedInputStream(socket.getInputStream()),
                    BufferedOutputStream(socket.getOutputStream())
                ) {
                    connections.removeAt(index)
                    viewModelScope.launch {
                        _recyclerState.emit(RecyclerState.ConnRemove(index))
                    }
                }.apply {
                    isDaemon = true
                }.start()

                viewModelScope.launch {
                    _recyclerState.emit(RecyclerState.ConnAdd(connections.lastIndex))
                }
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
            _recyclerState.emit(RecyclerState.LogAdd(logs.lastIndex))
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

                Thread(socketListener).start()
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
        TODO("Not yet implemented")
    }

    companion object {
        private val TAG = ServerViewModel::class.java.simpleName
    }
}