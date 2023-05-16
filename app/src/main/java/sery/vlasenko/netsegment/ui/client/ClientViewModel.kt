package sery.vlasenko.netsegment.ui.client

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.data.NetworkModule
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.test.PacketPing
import sery.vlasenko.netsegment.model.test.PacketPingAnswer
import sery.vlasenko.netsegment.ui.base.BaseRXViewModel
import sery.vlasenko.netsegment.ui.server.ServerUiState
import sery.vlasenko.netsegment.ui.server.SingleEvent
import sery.vlasenko.netsegment.ui.server.UiState
import sery.vlasenko.netsegment.ui.server.log.LogState
import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.toTimeFormat
import java.io.BufferedInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

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

    private var socket: Socket? = null

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

//        logs.add(LogItem(time, msg))

        viewModelScope.launch {
            _logState.emit(LogState.LogAdd(logs.lastIndex))
        }
    }

    fun onConnectClicked(ip: String, port: String) {
        Thread {
            socket = Socket()

            socket?.connect(InetSocketAddress(ip, port.toInt()))

            if (socket?.isConnected == true) {
                addMessageToLogs(getString(R.string.connected, port))
            }

            val input = socket!!.getInputStream()
            val output = socket!!.getOutputStream()

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    while (true) {
                        try {
                            val r = input.read()

                            if (r == -1) {

//                                break
                            } else if (r == PacketBuilder.PACKET_HEADER) {
                                val packetType = PacketType.fromByte(input.read().toByte())

//                                when (packetType) {
//                                    PacketType.PING_ANSWER -> handlePingAnswerPacket()
//                                    PacketType.PING -> handlePingPacket()
//                                    else -> onUnknownPacketType(packetType)
//                                }

                                if (packetType == PacketType.PING) {
                                    val byteArray = ByteArray(PacketPing.arraySize)

                                    input.read(byteArray)

                                    val receivedPacket = PacketPingAnswer.fromByteArray(byteArray)

                                    output.write(PacketBuilder.getPacketPingAnswer(receivedPacket.time).send())
                                }
                            }
                        } catch (e: IOException) {
                            println("Client exception" + e.message)
//                            break
                        }
                    }

                }
            }
        }.start()
    }

    fun onDisconnectClicked() {
        socket?.shutdownOutput()
        socket?.shutdownInput()
        socket?.close()
        socket = null
    }

}

fun main() {
    val packet = PacketBuilder.getPacketPing()


}