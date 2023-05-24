package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket

class UdpPacketMeasuresEnd(
) : NewPacket() {

    override fun send(): ByteArray = byteArrayOf(4, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0)

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")
        override val packetSize: Int
            get() = TODO("Not yet implemented")

        override fun fromByteArray(byteArray: ByteArray): UdpPacketMeasuresEnd =
            UdpPacketMeasuresEnd()
    }

}