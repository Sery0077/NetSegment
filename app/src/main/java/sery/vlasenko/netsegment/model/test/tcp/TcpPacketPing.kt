package sery.vlasenko.netsegment.model.test.tcp

import sery.vlasenko.netsegment.model.test.NewPacket

class TcpPacketPing(
    val isAnswer: Boolean = false
) : NewPacket() {

    override fun send(): ByteArray =
        if (isAnswer)
            byteArrayOf(9, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0)
        else
            byteArrayOf(9, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0)

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")

        override val packetSize: Int
            get() = 9

        override fun fromByteArray(byteArray: ByteArray): TcpPacketPing =
            TcpPacketPing(isAnswer = byteArray[0].toInt() == 2)

        fun isAnswer(byteArray: ByteArray): Boolean = byteArray[0].toInt() == 2
    }

}