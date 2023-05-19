package sery.vlasenko.netsegment.domain.packet

import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.PacketConnect
import sery.vlasenko.netsegment.model.test.PacketConnectAnswer
import sery.vlasenko.netsegment.utils.PacketFactory
import sery.vlasenko.netsegment.utils.PacketType
import java.net.DatagramPacket
import java.net.DatagramSocket

class UdpPacketHandler(
    private val socket: DatagramSocket,
) {

    fun handlePacket(
        packet: DatagramPacket,
        isNeedToResendPacket: Boolean = false,
        onPacketReceived: (packet: Packet) -> Unit = {},
        onUnknownPacket: (data: ByteArray) -> Unit = {},
    ) {
        val packetType = try {
            PacketType.fromByte(packet.data[1])
        } catch (e: IllegalArgumentException) {
            onUnknownPacket.invoke(packet.data)
            return
        }

        when (packetType) {
            PacketType.PING -> handlePingPacket(packet.data, onPacketReceived)
            PacketType.PING_ANSWER -> handlePingAnswerPacket(packet.data, onPacketReceived)
            PacketType.DATA -> handleDataPacket(
                packet.data,
                isNeedToResendPacket,
                onPacketReceived,
                onUnknownPacket
            )
            PacketType.SYS -> TODO()
            PacketType.CONNECT -> handleConnectPacket(
                packet,
                onPacketReceived,
                onUnknownPacket
            )
            PacketType.CONNECT_ANSWER -> handleConnectAnswerPacket(
                packet.data,
                onPacketReceived,
                onUnknownPacket
            )
            PacketType.DISCONNECT -> TODO()
            PacketType.SUSPEND -> TODO()
        }
    }

    private fun handlePingAnswerPacket(
        data: ByteArray,
        onPacketReceived: (packet: Packet) -> Unit
    ) {

    }

    private fun handleDataPacket(
        data: ByteArray,
        needToResendPacket: Boolean,
        onPacketReceived: (packet: Packet) -> Unit,
        onUnknownPacket: (data: ByteArray) -> Unit
    ) {

    }

    private fun handleConnectAnswerPacket(
        data: ByteArray,
        onPacketReceived: (packet: Packet) -> Unit,
        onUnknownPacket: (data: ByteArray) -> Unit
    ) {
        try {
            val b = data.sliceArray(2..PacketConnectAnswer.packetDataSize + 1)

            val receivedPacket = PacketConnectAnswer.fromByteArray(b)

            onPacketReceived(receivedPacket)
        } catch (e: Exception) {
            onUnknownPacket(data)
        }
    }

    private fun handleConnectPacket(
        packet: DatagramPacket,
        onPacketReceived: (packet: Packet) -> Unit,
        onUnknownPacket: (data: ByteArray) -> Unit
    ) {
        try {
            val b = packet.data.sliceArray(2..PacketConnect.packetDataSize + 1)

            val receivedPacket = PacketConnect.fromByteArray(b)

            val sentPacket = PacketFactory.getPacket(PacketType.CONNECT_ANSWER, receivedPacket.time)

            println("fefe" + packet.socketAddress.toString())

            socket.send(DatagramPacket(sentPacket.send(), sentPacket.send().size, packet.socketAddress))

            onPacketReceived(receivedPacket)

            println("fefe server send" + sentPacket.send().contentToString())
        } catch (e: Exception) {
            e.printStackTrace()
            println("fefe server exception")
//            onUnknownPacket(p)
        }
    }

    private fun handlePingPacket(data: ByteArray, onPacketReceived: (packet: Packet) -> Unit) {

    }

}