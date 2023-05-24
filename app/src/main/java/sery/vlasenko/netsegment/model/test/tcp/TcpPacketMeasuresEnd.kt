package sery.vlasenko.netsegment.model.test.tcp

import sery.vlasenko.netsegment.model.test.NewPacket

class TcpPacketMeasuresEnd: NewPacket() {

    override fun send(): ByteArray =
        byteArrayOf(5, 0, 4, 11, 0, 0, 0)

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")
        override val packetSize: Int
            get() = TODO("Not yet implemented")

        override fun fromByteArray(byteArray: ByteArray): TcpPacketMeasuresEnd =
            TcpPacketMeasuresEnd()
    }

}