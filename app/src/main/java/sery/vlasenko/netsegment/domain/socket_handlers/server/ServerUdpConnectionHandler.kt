package sery.vlasenko.netsegment.domain.socket_handlers.server

import sery.vlasenko.netsegment.domain.packet.UdpPacketHandler
import sery.vlasenko.netsegment.model.test.PacketConnect
import sery.vlasenko.netsegment.utils.PacketFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException

class ServerUdpConnectionHandler(
    private val socket: DatagramSocket,
    private val onConnectAdd: (packet: DatagramPacket) -> Unit,
    private val onConnectFail: () -> Unit = {}
): Thread() {

    init {
        isDaemon = true
    }

    private val packet = DatagramPacket(ByteArray(PacketConnect.packetSize), PacketConnect.packetSize)
    private val packetHandler = UdpPacketHandler(socket)

    override fun run() {
        while (true) {
            try {
                socket.receive(packet)

                println("fefe server receive" + packet.data.contentToString())

                if (packet.data[0] == PacketFactory.PACKET_HEADER.toByte()) {
                    packetHandler.handlePacket(
                        packet,
                        onPacketReceived = { packet ->
                            (packet as? PacketConnect)
                                ?.let {
                                    onConnectAdd(this.packet)
                                }
                        },
                        onUnknownPacket = {
                            println("fefe packet unknown")
                        }
                    )
                }
            } catch (e: SocketException) {
                println("fefe exception" + e.message)
                onConnectFail.invoke()
                break
            }
        }
    }

    override fun interrupt() {
        onConnectFail.invoke()
        super.interrupt()
    }

}