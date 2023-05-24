package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket

class UdpPacketMeasuresStart(
) : NewPacket() {

    override fun send(): ByteArray = byteArrayOf(4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0)

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")
        override val packetSize: Int
            get() = TODO("Not yet implemented")

        override fun fromByteArray(byteArray: ByteArray): UdpPacketMeasuresStart =
            UdpPacketMeasuresStart()
    }

}