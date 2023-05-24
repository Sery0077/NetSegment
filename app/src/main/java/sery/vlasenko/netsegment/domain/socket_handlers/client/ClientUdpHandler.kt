package sery.vlasenko.netsegment.domain.socket_handlers.client

import android.os.Looper
import sery.vlasenko.netsegment.model.test.udp.UdpPacketMeasuresAsk
import sery.vlasenko.netsegment.model.test.udp.UdpPacketPing
import sery.vlasenko.netsegment.ui.client.ClientHandlerCallback
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ClientUdpHandler(
    private val socket: DatagramSocket,
    private val callback: (data: ClientHandlerCallback) -> Unit = {},
) : Thread() {

    companion object {
        private val TAG = ClientUdpHandler::class.java.simpleName
    }

    private var isWorking = AtomicBoolean(true)

    private val pingAnswer = datagramPacketFromArray(UdpPacketPing(isAnswer = true).send())
    private val ping = datagramPacketFromArray(UdpPacketPing(isAnswer = false).send())
    private val measuresAsk = datagramPacketFromArray(UdpPacketMeasuresAsk().send())

    private val buf = DatagramPacket(ByteArray(1500), 1500)

    @Volatile
    var lastTimePingSend = AtomicLong(0)

    override fun run() {
        socket.soTimeout = 2000

        while (isWorking.get()) {
            try {
                synchronized(socket) {
                    socket.receive(buf)

                    when (buf.data[0].toInt()) {
                        1 -> {
                            socket.send(pingAnswer)
                            socket.send(ping)
                            lastTimePingSend.set(System.currentTimeMillis())
                        }
                        2 -> {
                            handlePing()
                        }
                        4 -> {
                            if (buf.data[5].toInt() == 1) {
                                socket.send(measuresAsk)
                                sendCallback(ClientHandlerCallback.MeasuresStart)
                            } else if (buf.data[5].toInt() == 10) {
                                sendCallback(ClientHandlerCallback.MeasuresEnd)
                            }
                        }
                        3 -> {
                            socket.send(buf)
                        }
                    }
                }
            } catch (e: SocketException) {
                break
            } catch (e: SocketTimeoutException) {
                sendCallback(ClientHandlerCallback.Timeout)
            }
        }
    }

    private fun handlePing() {
        val ping = System.currentTimeMillis() - lastTimePingSend.get()

        sendCallback(ClientHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ClientHandlerCallback) {
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }
    }

    private fun trySleep(s: Long) {
        try {
            sleep(s)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

    override fun interrupt() {
        isWorking.set(false)
        super.interrupt()
    }
}