package sery.vlasenko.netsegment.domain.socket_handlers.server.udp

import android.os.Handler
import android.os.Looper
import sery.vlasenko.netsegment.model.test.udp.UdpPacketConnect
import sery.vlasenko.netsegment.model.test.udp.UdpPacketType
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import sery.vlasenko.netsegment.utils.datagramPacketFromSize
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException

class ServerUdpConnectionHandler(
    private val socket: DatagramSocket,
    private val onConnectAdd: (packet: DatagramPacket) -> Unit
) : MyThread() {

    private val packetConnectAnswer =
        datagramPacketFromArray(UdpPacketConnect(isAnswer = true).send())

    private val buf = datagramPacketFromSize(UdpPacketConnect.packetSize)

    override fun run() {
        socket.soTimeout = 5

        while (!isInterrupted) {
            try {
                socket.receive(buf)

                when (buf.data[0]) {
                    UdpPacketType.CONNECT.typeByte -> {
                        interrupt()
                        socket.send(packetConnectAnswer.apply { socketAddress = buf.socketAddress })
                        Handler(Looper.getMainLooper()).post { onConnectAdd(buf) }
                    }
                }
            } catch (e: SocketException) {
                interrupt()
            } catch (e: SocketTimeoutException) {
                trySleep(50L)
            }
        }
    }

}