package sery.vlasenko.netsegment.domain

import sery.vlasenko.netsegment.model.test.tcp.TcpPacketData
import sery.vlasenko.netsegment.model.test.udp.UdpPacketData
import kotlin.random.Random

class PacketFactory {

    fun getTcpPacketData(dataSize: Int) =
        TcpPacketData(
            dataSize = dataSize,
            data = Random.nextBytes(dataSize - 1)
        )

    fun getUdpPacketData(dataSize: Int) =
        UdpPacketData(
            dataSize = dataSize,
            data = Random.nextBytes(dataSize - UdpPacketData.packetHeaderSize)
        )

}