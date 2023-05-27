package sery.vlasenko.netsegment.domain

import sery.vlasenko.netsegment.model.test.tcp.TcpPacketData
import kotlin.random.Random

class PacketFactory {

    fun getPacketData(dataSize: Int) =
        TcpPacketData(
            dataSize = dataSize,
            data = Random.nextBytes(dataSize - 1)
        )

}