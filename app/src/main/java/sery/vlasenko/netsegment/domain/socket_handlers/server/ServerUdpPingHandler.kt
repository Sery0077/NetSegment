package sery.vlasenko.netsegment.domain.socket_handlers.server

import android.os.Looper
import sery.vlasenko.netsegment.model.test.udp.UdpPacketPing
import sery.vlasenko.netsegment.ui.server.ServerPingHandlerCallback
import sery.vlasenko.netsegment.utils.TimeConst.PING_TIMEOUT
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicLong

class ServerUdpPingHandler(
    private val socket: DatagramSocket,
    private val callback: (data: ServerPingHandlerCallback) -> Unit = {},
) : Thread() {

    companion object {
        private val TAG = ServerTcpPingHandler::class.java.simpleName
    }

    init {
        isDaemon = true
    }

    private val pingAnswer = datagramPacketFromArray(UdpPacketPing(isAnswer = true).send()).apply {
//        socketAddress = addr
    }
    private val ping = datagramPacketFromArray(UdpPacketPing(isAnswer = false).send()).apply {
//        socketAddress = addr
    }

    private val buf = DatagramPacket(ByteArray(1500), 1500)

    @Volatile
    var lastTimePingSend = AtomicLong(0)

    private val pingInterval = 100L

    private fun handlePing() {
        val ping = System.currentTimeMillis() - lastTimePingSend.get()

        sendCallback(ServerPingHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ServerPingHandlerCallback) {
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }
    }


    private fun sendPing() {
        socket.send(ping)
        lastTimePingSend.set(System.currentTimeMillis())
    }

    override fun run() {
        socket.soTimeout = PING_TIMEOUT

        sendPing()

        while (!isInterrupted) {
            try {
                socket.receive(buf)

                when (buf.data[0].toInt()) {
                    1 -> {
                        socket.send(pingAnswer)
                    }
                    2 -> {

                        handlePing()

                        trySleep(1000)

                        sendPing()
                    }
                    7 -> {

                        sendCallback(ServerPingHandlerCallback.ConnectionClose)
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

    private fun trySleep(s: Long) {
        try {
            sleep(s)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

}