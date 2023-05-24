package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketPing

class UdpPacketPing(
    val isAnswer: Boolean = false
) : NewPacket() {

    override fun send(): ByteArray =
        if (isAnswer)
            byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        else
            byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")
        override val packetSize: Int
            get() = TODO("Not yet implemented")

        override fun fromByteArray(byteArray: ByteArray): UdpPacketPing =
            UdpPacketPing(isAnswer = byteArray[0].toInt() == 2)
    }

}