package sery.vlasenko.netsegment.model.test.tcp

import sery.vlasenko.netsegment.model.test.Packet

class TcpPacketMeasuresStart : Packet() {

    override val packetDataSize: Int
        get() = 5

    override val packetSize: Int
        get() = 7

    override fun send(): ByteArray =
        byteArrayOf(5, 0, 4, 2, 0, 0, 0)

    companion object Builder : PacketBuilder {

        val packetSize = 7

        override fun fromByteArray(byteArray: ByteArray): TcpPacketMeasuresStart =
            TcpPacketMeasuresStart()
    }

}