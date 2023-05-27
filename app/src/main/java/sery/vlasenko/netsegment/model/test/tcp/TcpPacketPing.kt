package sery.vlasenko.netsegment.model.test.tcp

import sery.vlasenko.netsegment.model.test.NewPacket

class TcpPacketPing(
    val isAnswer: Boolean = false
) : NewPacket() {

    override val packetDataSize: Int
        get() = 9

    override val packetSize: Int
        get() = 11

    override fun send(): ByteArray =
        if (isAnswer)
            byteArrayOf(9, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0)
        else
            byteArrayOf(9, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0)

    companion object Builder : PacketBuilder {

        override fun fromByteArray(byteArray: ByteArray): TcpPacketPing =
            TcpPacketPing(isAnswer = byteArray[0].toInt() == 2)

        fun isAnswer(byteArray: ByteArray): Boolean = byteArray[0].toInt() == 2
    }

}