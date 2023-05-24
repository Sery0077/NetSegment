package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket

class UdpPacketDisconnect: NewPacket() {

    override fun send(): ByteArray =
            byteArrayOf(7, 0, 0, 0, 0)

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")

        override val packetSize: Int
            get() = 5

        override fun fromByteArray(byteArray: ByteArray): UdpPacketDisconnect =
            UdpPacketDisconnect()
    }

}