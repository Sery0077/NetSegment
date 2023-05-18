package sery.vlasenko.netsegment.domain.socket_handlers.client

import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.test.PacketConnect
import sery.vlasenko.netsegment.utils.PacketFactory
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.UdpHelper
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException

class ClientUdpConnectionHandler(
    private val socket: DatagramSocket,
    private val onConnectSuccess: (socket: Connection<DatagramSocket>) -> Unit,
    private val onConnectFail: () -> Unit = {}
): Thread() {

    init {
        isDaemon = true
    }

    private val packet = UdpHelper.udpPacketFromSize(PacketConnect.packetSize)

    override fun run() {
        for (t in 0..4) {
            try {
                val sentPacket = PacketFactory.getPacket(PacketType.CONNECT)

                socket.send(UdpHelper.udpPacketFromSize(PacketConnect.packetSize).apply {
                    data = sentPacket.send() }
                )

                socket.receive(packet)
            } catch (e: SocketException) {
                println("fefe exception" + e.message)
                onConnectFail.invoke()
                break
            }
        }
        while (true) {
            try {
                socket.receive(packet)

//                if (packet.data[0] == PacketFactory.PACKET_HEADER.toByte()) {
//
//                }

                println("fefe packet" + packet.data.contentToString())

//                if (socket != null) {
////                    onConnectionAdd.invoke(socket)
//                }
            } catch (e: SocketException) {
                println("fefe exception" + e.message)
                onConnectFail.invoke()
                break
            }
            sleep(1000)
        }
    }

    override fun interrupt() {
        onConnectFail.invoke()
        super.interrupt()
    }

}