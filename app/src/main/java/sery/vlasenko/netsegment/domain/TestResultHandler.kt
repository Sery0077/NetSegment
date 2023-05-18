package sery.vlasenko.netsegment.domain

import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.TestResult
import java.util.*

class TestResultHandler {

    private val pings = mutableListOf<Int>()

    private val sentPackets = mutableListOf<Packet>()
    private val receivedPackets = mutableListOf<Packet>()

    fun handlePackets(sendPacket: Packet, receivedPacket: Packet?) {
        sentPackets.add(sendPacket)
        receivedPacket
            ?.let { recvPacket ->
                pings.add((Calendar.getInstance().timeInMillis - recvPacket.time).toInt())
                receivedPackets.add(receivedPacket)
            }
    }

    fun getResult(): TestResult = TestResult(
        averagePing = pings.average().toInt(),
        jitter = pings.max() - pings.min(),
        sentPacketCount = sentPackets.size,
        receivedPacket = receivedPackets.size,
        lossPacket = sentPackets.size - receivedPackets.size
    )

}