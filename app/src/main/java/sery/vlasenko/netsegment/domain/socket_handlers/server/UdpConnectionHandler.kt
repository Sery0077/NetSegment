package sery.vlasenko.netsegment.domain.socket_handlers.server

import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.test.PacketConnect
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException

class UdpConnectionHandler(
    private val socket: DatagramSocket,
    private val onConnectSuccess: (socket: Connection<DatagramSocket>) -> Unit,
    private val onConnectFail: () -> Unit = {}
): Thread() {

    init {
        isDaemon = true
    }

    private val packet = DatagramPacket(ByteArray(PacketConnect.packetDataSize), PacketConnect.packetDataSize)

    override fun run() {
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