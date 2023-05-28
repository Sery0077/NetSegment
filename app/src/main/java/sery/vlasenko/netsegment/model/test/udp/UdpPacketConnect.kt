package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.Packet

class UdpPacketConnect(
    val isAnswer: Boolean,
) : Packet() {

    override val packetDataSize: Int
        get() = 0

    override val packetSize: Int
        get() = 5

    override fun send(): ByteArray =
        if (isAnswer)
            byteArrayOf(6, 0, 0, 0, 0)
        else
            byteArrayOf(5, 0, 0, 0, 0)

    companion object Builder : PacketBuilder {

        const val packetSize = 5

        override fun fromByteArray(byteArray: ByteArray): UdpPacketConnect =
            UdpPacketConnect(isAnswer = byteArray[0].toInt() == 6)
    }

}