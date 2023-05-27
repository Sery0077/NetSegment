package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket

class UdpPacketMeasuresAsk : NewPacket() {

    override val packetDataSize: Int
        get() = 4

    override val packetSize: Int
        get() = 9

    override fun send(): ByteArray = byteArrayOf(4, 0, 0, 0, 0, 1, 0, 0, 0)

    companion object Builder : PacketBuilder {
        override fun fromByteArray(byteArray: ByteArray): UdpPacketMeasuresAsk =
            UdpPacketMeasuresAsk()
    }

}