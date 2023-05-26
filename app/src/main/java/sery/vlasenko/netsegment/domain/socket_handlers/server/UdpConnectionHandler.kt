package sery.vlasenko.netsegment.domain.socket_handlers.server

import android.os.Handler
import android.os.Looper
import sery.vlasenko.netsegment.model.test.udp.UdpPacketConnect
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import sery.vlasenko.netsegment.utils.datagramPacketFromSize
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException

class UdpConnectionHandler(
    private val socket: DatagramSocket,
    private val onConnectAdd: (packet: DatagramPacket) -> Unit,
    private val onConnectFail: () -> Unit = {}
) : Thread() {

    init {
        isDaemon = true
    }

    private val packetConnectAnswer =
        datagramPacketFromArray(UdpPacketConnect(isAnswer = true).send())

    private val buf = datagramPacketFromSize(1500)

    override fun run() {
        socket.soTimeout = 5

        while (!isInterrupted) {
            try {
                socket.receive(buf)

                when (buf.data[0].toInt()) {
                    5 -> {
                        socket.send(packetConnectAnswer.apply { socketAddress = buf.socketAddress })
                        Handler(Looper.getMainLooper()).post { onConnectAdd(buf) }
                    }
                }
            } catch (e: SocketException) {
                onConnectFail.invoke()
                break
            } catch (e: SocketTimeoutException) {
                trySleep(50L)
            }
        }
    }

    private fun trySleep(s: Long) {
        try {
            sleep(s)
        } catch (e: InterruptedException) {
            interrupt()
        }
    }

}