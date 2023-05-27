package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.NewPacket

class UdpPacketDisconnect : NewPacket() {

    override val packetDataSize: Int
        get() = 0

    override val packetSize: Int
        get() = 5

    override fun send(): ByteArray =
        byteArrayOf(7, 0, 0, 0, 0)

    companion object Builder : PacketBuilder {

        override fun fromByteArray(byteArray: ByteArray): UdpPacketDisconnect =
            UdpPacketDisconnect()
    }

}