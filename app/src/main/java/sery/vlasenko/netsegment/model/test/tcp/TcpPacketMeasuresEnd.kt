package sery.vlasenko.netsegment.model.test.tcp

import sery.vlasenko.netsegment.model.test.NewPacket

class TcpPacketMeasuresEnd : NewPacket() {

    override val packetDataSize: Int
        get() = 5

    override val packetSize: Int
        get() = 7

    override fun send(): ByteArray =
        byteArrayOf(5, 0, 4, 11, 0, 0, 0)

    companion object Builder : PacketBuilder {

        override fun fromByteArray(byteArray: ByteArray): TcpPacketMeasuresEnd =
            TcpPacketMeasuresEnd()
    }

}