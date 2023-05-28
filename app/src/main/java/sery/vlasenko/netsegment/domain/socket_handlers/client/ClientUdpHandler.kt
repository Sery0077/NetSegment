package sery.vlasenko.netsegment.domain.socket_handlers.client

import android.os.Looper
import okio.IOException
import sery.vlasenko.netsegment.model.test.udp.UdpPacketMeasuresAsk
import sery.vlasenko.netsegment.model.test.udp.UdpPacketPing
import sery.vlasenko.netsegment.model.test.udp.UdpPacketType
import sery.vlasenko.netsegment.ui.client.ClientHandlerCallback
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.Timeouts.PING_TIMEOUT
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import sery.vlasenko.netsegment.utils.datagramPacketFromSize
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ClientUdpHandler(
    private val socket: DatagramSocket,
    private val callback: (data: ClientHandlerCallback) -> Unit = {},
) : MyThread() {

    companion object {
        private val TAG = ClientUdpHandler::class.java.simpleName
    }

    private val pingAnswer = datagramPacketFromArray(UdpPacketPing(isAnswer = true).send())
    private val ping = datagramPacketFromArray(UdpPacketPing(isAnswer = false).send())

    private val measuresAsk = datagramPacketFromArray(UdpPacketMeasuresAsk().send())

    private val buf = datagramPacketFromSize(1500)

    private val lastTimePingSend = AtomicLong(0)

    private val isPinging = AtomicBoolean(true)

    private val pingThread = Thread {
        var lastTimePing = 0L

        while (!isInterrupted) {
            if (System.currentTimeMillis() - lastTimePing > 200) {
                try {
                    sendPing()
                } catch (e: SocketException) {
                    sendCallback(ClientHandlerCallback.SocketClose)
                    interrupt()
                }

                lastTimePing = System.currentTimeMillis()
            }
        }
    }

    private fun sendPing() {
        socket.send(ping)

        lastTimePingSend.set(System.nanoTime())
    }

    override fun run() {
        socket.soTimeout = PING_TIMEOUT

        pingThread.start()

        while (!isInterrupted) {
            try {
                socket.receive(buf)

                when (buf.data[0]) {
                    UdpPacketType.PING.typeByte -> {
                        socket.send(pingAnswer)
                    }
                    UdpPacketType.PING_ANSWER.typeByte -> {
                        handlePing()
                    }
                    UdpPacketType.MEASURES.typeByte -> {
                        when (buf.data[5]) {
                            UdpPacketType.MEASURES_ASK.subTypeByte -> {
                                socket.send(measuresAsk)
                            }
                            UdpPacketType.MEASURES_START.subTypeByte -> {
                                isPinging.set(false)
                                sendCallback(ClientHandlerCallback.MeasuresStart)
                            }
                            UdpPacketType.MEASURES_END.subTypeByte -> {
                                isPinging.set(true)
                                sendCallback(ClientHandlerCallback.MeasuresEnd)
                            }
                        }
                    }
                    UdpPacketType.DISCONNECT.typeByte -> {
                        sendCallback(ClientHandlerCallback.SocketClose)
                        interrupt()
                    }
                    UdpPacketType.DATA.typeByte -> {
                        socket.send(buf)
                    }
                }
            } catch (e: SocketException) {
                sendCallback(ClientHandlerCallback.SocketClose)
                interrupt()
                break
            } catch (e: SocketTimeoutException) {
                sendCallback(ClientHandlerCallback.Timeout)
            } catch (e: IOException) {
                sendCallback(ClientHandlerCallback.SocketClose)
                interrupt()
                break
            }
        }
    }

    private fun handlePing() {
        val ping = System.nanoTime() - lastTimePingSend.get()

        sendCallback(ClientHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ClientHandlerCallback) {
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }
    }

    override fun interrupt() {
        isPinging.set(false)
        pingThread.interrupt()
        super.interrupt()
    }

}