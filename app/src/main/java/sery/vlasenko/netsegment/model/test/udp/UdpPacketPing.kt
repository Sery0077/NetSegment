package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket

class UdpPacketPing(
    val isAnswer: Boolean = false
) : NewPacket() {

    override val packetDataSize: Int
        get() = 8

    override val packetSize: Int
        get() = 13

    override fun send(): ByteArray =
        if (isAnswer)
            byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        else
            byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    companion object Builder : PacketBuilder {

        override fun fromByteArray(byteArray: ByteArray): UdpPacketPing =
            UdpPacketPing(isAnswer = byteArray[0].toInt() == 2)
    }

}