package sery.vlasenko.netsegment.domain.socket_handlers.client

import android.os.Looper
import okio.IOException
import sery.vlasenko.netsegment.domain.socket_handlers.UdpPingThread
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

class ClientUdpHandler(
    private val socket: DatagramSocket,
    private val callback: (data: ClientHandlerCallback) -> Unit = {},
) : MyThread() {

    companion object {
        private val TAG = ClientUdpHandler::class.java.simpleName
    }

    private val pingAnswer = datagramPacketFromArray(UdpPacketPing(isAnswer = true).send())

    private val measuresAsk = datagramPacketFromArray(UdpPacketMeasuresAsk().send())

    private val buf = datagramPacketFromSize(1500)

    private val pingThread = UdpPingThread(socket) {
        sendCallback(ClientHandlerCallback.SocketClose)
        interrupt()
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
                                pingThread.stopPing()
                                sendCallback(ClientHandlerCallback.MeasuresStart)
                            }
                            UdpPacketType.MEASURES_END.subTypeByte -> {
                                pingThread.startPing()
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
        val ping = System.nanoTime() - pingThread.lastTimePingSend.get()

        sendCallback(ClientHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ClientHandlerCallback) {
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }
    }

    override fun interrupt() {
        pingThread.stopPing()
        pingThread.interrupt()
        super.interrupt()
    }

}