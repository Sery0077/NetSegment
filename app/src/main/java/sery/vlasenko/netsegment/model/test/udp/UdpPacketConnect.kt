package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket

class UdpPacketConnect(
    val isAnswer: Boolean,
) : NewPacket() {

    override fun send(): ByteArray =
        if (isAnswer)
            byteArrayOf(6, 0, 0, 0, 0)
        else
            byteArrayOf(5, 0, 0, 0, 0)

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")

        override val packetSize: Int
            get() = 5

        override fun fromByteArray(byteArray: ByteArray): UdpPacketConnect =
            UdpPacketConnect(isAnswer = byteArray[0].toInt() == 6)
    }

}