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
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.utils.toTimeFormat
import java.net.ServerSocket
import java.util.*

class ServerViewModel : BaseRXViewModel() {

    private val _ipState: MutableLiveData<ServerUiState> = MutableLiveData(ServerUiState.Loading)
    val ipState: LiveData<ServerUiState>
        get() = _ipState

    val logs: MutableList<LogItem> = mutableListOf()

    private val _logState: MutableSharedFlow<LogState> = MutableSharedFlow(
        replay = 10,
        extraBufferCapacity = 10,
    )
    val logState: SharedFlow<LogState>
        get() = _logState

    private val _singleEvent: MutableLiveData<SingleEvent> = MutableLiveData()
    val singleEvent: LiveData<SingleEvent>
        get() = _singleEvent

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

    private fun addMessageToLogs(msg: String) {
        val rawTime = Calendar.getInstance().timeInMillis

        val time = rawTime.toTimeFormat()

        logs.add(LogItem(time, msg))

        viewModelScope.launch {
            _logState.emit(LogState.LogAdd(logs.lastIndex))
        }
    }

    private var socket: ServerSocket? = null

    fun onOpenSocketClicked(port: String) {
        if (!validatePort(port)) {
            _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.incorrect_port))
            return
        }

        if (socket != null && !socket!!.isClosed) {
            _singleEvent.value = SingleEvent.ShowToastEvent(getString(R.string.socket_alreade_opened))
        } else {
            try {
                socket = ServerSocket(port.toInt())
                addMessageToLogs(getString(R.string.socket_opened, port))
            } catch (e: Exception) {
                addMessageToLogs("Ошибка открытия сокета ${e.message}")
            }
        }
    }

    fun onCloseSocketClicked() {
        val port = socket?.localPort ?: 0
        closeSocket()

        val msg = getString(R.string.socket_closed, port)
        addMessageToLogs(msg)
        _singleEvent.value = SingleEvent.ShowToastEvent(msg)
    }

    private fun closeSocket() {
        socket?.close()
        socket = null
    }
    private fun validatePort(port: String): Boolean {
        return port.toIntOrNull() != null
    }

    override fun onCleared() {
        super.onCleared()
        closeSocket()
    }

}