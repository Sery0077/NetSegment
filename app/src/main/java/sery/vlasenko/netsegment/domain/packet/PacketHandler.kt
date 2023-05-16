package sery.vlasenko.netsegment.domain.packet

import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.PacketPing
import sery.vlasenko.netsegment.model.test.PacketPingAnswer
import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.PacketType
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class PacketHandler(
    private val socket: Socket,
    private val input: InputStream = socket.getInputStream(),
    private val output: OutputStream = socket.getOutputStream(),
) {

    fun handlePacket(
        firstByte: Int,
        onPacketReceived: (packet: Packet) -> Unit = {}
    ) {
        when (PacketType.fromByte(firstByte.toByte())) {
            PacketType.PING -> handlePingPacket(input, onPacketReceived)
            PacketType.PING_ANSWER -> handlePingAnswerPacket(input, onPacketReceived)
            PacketType.DATA -> TODO()
            PacketType.SYS -> TODO()
            PacketType.CONNECT -> TODO()
            PacketType.CONNECT_ANSWER -> TODO()
            PacketType.DISCONNECT -> TODO()
            PacketType.SUSPEND -> TODO()
        }
    }

    private fun handlePingPacket(input: InputStream, onPacketReceived: (packet: Packet) -> Unit) {
        val byteArray = ByteArray(PacketPing.arraySize)

        input.read(byteArray)

        val receivedPacket = PacketPing.fromByteArray(byteArray)

        output.write(PacketBuilder.getPacketPingAnswer(receivedPacket.time).send())

        onPacketReceived.invoke(receivedPacket)
    }

    private fun handlePingAnswerPacket(
        input: InputStream,
        onPacketReceived: (packet: Packet) -> Unit
    ) {
        val byteArray = ByteArray(PacketPing.arraySize)

        input.read(byteArray)

        val receivedPacket = PacketPingAnswer.fromByteArray(byteArray)

        onPacketReceived.invoke(receivedPacket)
    }
}