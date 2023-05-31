package sery.vlasenko.netsegment.domain.socket_handlers.server.udp

import android.os.Looper
import sery.vlasenko.netsegment.domain.socket_handlers.UdpPingThread
import sery.vlasenko.netsegment.model.test.udp.UdpPacketPing
import sery.vlasenko.netsegment.model.test.udp.UdpPacketType
import sery.vlasenko.netsegment.ui.server.ServerPingHandlerCallback
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.Timeouts.PING_TIMEOUT
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import sery.vlasenko.netsegment.utils.datagramPacketFromSize
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException

class ServerUdpPingHandler(
    private val socket: DatagramSocket,
    private val callback: (data: ServerPingHandlerCallback) -> Unit = {},
) : MyThread() {

    companion object {
        private val TAG = ServerUdpPingHandler::class.java.simpleName
    }

    init {
        isDaemon = true
    }

    private val pingAnswer = datagramPacketFromArray(UdpPacketPing(isAnswer = true).send())

    private val buf = datagramPacketFromSize(100)

    private val pingThread = UdpPingThread(socket) {
        sendCallback(ServerPingHandlerCallback.ConnectionClose)
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
                    UdpPacketType.DISCONNECT.typeByte -> {
                        sendCallback(ServerPingHandlerCallback.ConnectionClose)
                        interrupt()
                    }
                }
            } catch (e: SocketException) {
                sendCallback(ServerPingHandlerCallback.ConnectionClose)
                break
            } catch (e: SocketTimeoutException) {
                sendCallback(ServerPingHandlerCallback.Timeout)
            }
        }
    }

    private fun handlePing() {
        val ping = System.nanoTime() - pingThread.lastTimePingSend.get()
        sendCallback(ServerPingHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ServerPingHandlerCallback) {
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