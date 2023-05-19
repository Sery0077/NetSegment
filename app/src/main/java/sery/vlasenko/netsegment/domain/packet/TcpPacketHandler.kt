package sery.vlasenko.netsegment.domain.packet

import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.PacketData
import sery.vlasenko.netsegment.model.test.PacketPing
import sery.vlasenko.netsegment.model.test.PacketPingAnswer
import sery.vlasenko.netsegment.utils.PacketFactory
import sery.vlasenko.netsegment.utils.PacketType
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer

class TcpPacketHandler(
    socket: Socket,
) {

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    fun handlePacket(
        isNeedToResendPacket: Boolean = false,
        firstByte: Int,
        onPacketReceived: (packet: Packet) -> Unit = {},
        onUnknownPacket: (packet: PacketType) -> Unit = {},
    ) {
        val packetType = try {
            PacketType.fromByte(firstByte.toByte())
        } catch (e: IllegalArgumentException) {
            onUnknownPacket.invoke(PacketType.DISCONNECT)
            return
        }

        when (packetType) {
            PacketType.PING -> handlePingPacket(onPacketReceived)
            PacketType.PING_ANSWER -> handlePingAnswerPacket(onPacketReceived)
            PacketType.DATA -> handleDataPacket(isNeedToResendPacket, onPacketReceived)
            PacketType.SYS -> TODO()
            PacketType.CONNECT -> TODO()
            PacketType.CONNECT_ANSWER -> TODO()
            PacketType.DISCONNECT -> TODO()
            PacketType.SUSPEND -> TODO()
        }
    }

    private fun handlePingPacket(onPacketReceived: (packet: Packet) -> Unit) {
        val byteArray = ByteArray(PacketPing.packetDataSize)

        input.read(byteArray)

        val receivedPacket = PacketPing.fromByteArray(byteArray)

        output.write(PacketFactory.getPacketPingAnswer(receivedPacket.time).send())

        onPacketReceived.invoke(receivedPacket)
    }

    private fun handlePingAnswerPacket(onPacketReceived: (packet: Packet) -> Unit) {
        val byteArray = ByteArray(PacketPingAnswer.packetDataSize)

        input.read(byteArray)

        val receivedPacket = PacketPingAnswer.fromByteArray(byteArray)

        onPacketReceived.invoke(receivedPacket)
    }

    private fun handleDataPacket(isNeedToResendPacket: Boolean, onPacketReceived: (packet: Packet) -> Unit) {
        val b = DataInputStream(input)

        val time = b.readLong()
        val dataSize = b.readInt()

        val data = ByteArray(dataSize)
        b.read(data)

        val buffer = ByteBuffer.allocate(PacketData.packetDataSize + dataSize)

        buffer.putLong(time)
        buffer.putInt(dataSize)
        buffer.put(data)

        val receivedPacket = PacketData.fromByteArray(buffer.array())

        if (isNeedToResendPacket) {
            output.write(receivedPacket.send())
        }

        onPacketReceived.invoke(receivedPacket)
    }
}