package sery.vlasenko.netsegment.domain.socket_handlers.client

import okio.IOException
import sery.vlasenko.netsegment.domain.packet.PacketHandler
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.PacketType
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

class ClientTcpHandler(
    private val socket: Socket,
    var onPacketReceived: (packet: Packet) -> Unit = {},
    var onUnknownPacketType: (packetType: PacketType) -> Unit = {},
    val onClose: () -> Unit = {},
) : Thread() {

    private val input: InputStream
        get() = socket.getInputStream()

    private val output: OutputStream
        get() = socket.getOutputStream()

    override fun run() {
        while (true) {
            try {
                val c = input.read()
                val firstByte = input.read()

                if (c == PacketBuilder.PACKET_HEADER) {
                    PacketHandler(socket).handlePacket(firstByte) {
                        onPacketReceived.invoke(it)
                    }
                } else if (c == -1) {
                    onClose.invoke()
//                    onUnknownPacketType.invoke(PacketType.fromByte(firstByte))
                }
            } catch (e: IOException) {
                println("fefe ${e.message}")
//                onClose.invoke()
            } catch (e: SocketException) {
                println("fefe ${e.message}")
            }
        }
    }

}