package sery.vlasenko.netsegment.domain.socket_handlers.client

import sery.vlasenko.netsegment.domain.packet.UdpPacketHandler
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.PacketConnect
import sery.vlasenko.netsegment.model.test.PacketConnectAnswer
import sery.vlasenko.netsegment.utils.PacketFactory
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.TimeConst
import sery.vlasenko.netsegment.utils.UdpHelper
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.*

class ClientUdpConnectionHandler(
    private val socketAddress: InetSocketAddress,
    private val socket: DatagramSocket,
    private val onConnectSuccess: (packet: PacketConnectAnswer) -> Unit,
    private val onConnectFail: (e: Exception?) -> Unit = {},
    private val onTryConnect: () -> Unit = {},
    private val onUnknownPacket: (packet: Any?) -> Unit = {},
    private val onTimeout: () -> Unit = {}
) : Thread() {

    init {
        isDaemon = true
    }

    private val packet = UdpHelper.udpPacketFromSize(PacketConnect.packetSize)
    private val packetHandler = UdpPacketHandler(socket)

    override fun run() {
        socket.soTimeout = TimeConst.CONNECTION_TIMEOUT.toInt()

        for (t in 0..4) {
            try {
                val sentPacket = PacketFactory.getPacket(PacketType.CONNECT)

                val datagramPacket = DatagramPacket(sentPacket.send(), sentPacket.send().size, socketAddress)

                socket.send(datagramPacket)

                onTryConnect.invoke()

                var receivedPacket: PacketConnectAnswer? = null

                println("fefe client send")

                socket.receive(packet)

                if (packet.data[0] == PacketFactory.PACKET_HEADER.toByte()) {
                    packetHandler.handlePacket(
                        packet,
                        onPacketReceived = { packet ->
                            (packet as? PacketConnectAnswer)
                                ?.let {
                                    receivedPacket = it
                                    onConnectSuccess(it)
                                }
                                ?: onUnknownPacket.invoke(packet)
                        },
                        onUnknownPacket = {
                            onUnknownPacket.invoke(it)
                            println("fefe client unknown")
                        }
                    )
                }

                if (receivedPacket != null) return

            } catch (e: SocketTimeoutException) {
                onConnectFail.invoke(e)
            }
        }
    }

    override fun interrupt() {
        onConnectFail.invoke(null)
        super.interrupt()
    }

}