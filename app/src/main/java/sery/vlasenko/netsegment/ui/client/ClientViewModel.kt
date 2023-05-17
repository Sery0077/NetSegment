package sery.vlasenko.netsegment.ui.client

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
import sery.vlasenko.netsegment.domain.socket_handlers.client.ClientTcpHandler
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.model.connections.TcpConnection
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.ServerUiState
import sery.vlasenko.netsegment.ui.server.SingleEvent
import sery.vlasenko.netsegment.ui.server.UiState
import sery.vlasenko.netsegment.ui.server.log.LogState
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket

class ClientViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    val logs: MutableList<LogItem> = mutableListOf()

    private val _logState: MutableSharedFlow<LogState> = MutableSharedFlow()
    val logState: SharedFlow<LogState>
        get() = _logState

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.SocketClosed)
    val uiState: LiveData<UiState>
        get() = _uiState

    private val _singleEvent: MutableLiveData<SingleEvent> = MutableLiveData()
    val singleEvent: LiveData<SingleEvent>
        get() = _singleEvent

    private var conn: Connection<*>? = null

    init {
//        getIp()
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
                        addMessageToLogs("${getString(R.string.ip_getted)} $ip")
                    },
                    onError = {
                        _ipState.value = ServerUiState.Error(it.message)
                        addMessageToLogs("${getString(R.string.ip_getting_error)} ${it.message}")
                    }
                )
        )
    }

    private fun addMessageToLogs(msg: String) {
        logs.add(LogItem(message = msg))

        viewModelScope.launch {
            _logState.emit(LogState.LogAdd(logs.lastIndex))
        }
    }

    private fun openTcpSocket(ip: String, port: String) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port.toInt()))

            if (socket.isConnected) {
                addMessageToLogs(getString(R.string.connected, "$ip $port"))
                _uiState.value = UiState.SocketOpened

                conn = TcpConnection(socket, getHandler(socket)).apply {
                    handler?.start()
                }
            }
        } catch (e: ConnectException) {
            addMessageToLogs(getString(R.string.connect_error, "$ip $port"))
        }
    }

    private fun getHandler(socket: Socket): ClientTcpHandler {
        return ClientTcpHandler(
            socket,
            onClose = this::closeSocket
        )
    }

    private fun closeSocket() {
        conn?.handler?.interrupt()
        conn?.handler = null

        (conn?.socket as? Socket)?.close()

        conn = null

        _uiState.postValue(UiState.SocketClosed)
    }

    fun onConnectClicked(ip: String, port: String, protocol: Protocol) {
        if (protocol == Protocol.TCP) {
            openTcpSocket(ip, port)
        } else {
            // TODO add udp protocol
        }
    }

    fun onDisconnectClicked() {
        closeSocket()
    }

    override fun onCleared() {
        closeSocket()
        super.onCleared()
    }

}