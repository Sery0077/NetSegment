package sery.vlasenko.netsegment.domain

import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.TestResult
import sery.vlasenko.netsegment.utils.plusValueOrPut
import kotlin.math.max
import kotlin.math.min

class TestResultHandler {

    private val sentPacketsCountBySize = linkedMapOf<Int, Int>()
    private val receivedPacketsBySize = linkedMapOf<Int, Int>()

    private val delaysBySize = linkedMapOf<Int, MutableList<Int>>()

    private var maxPing = 0
    private var minPing = 0

    private var sentPacketCount = 0
    private var receivedPacketCount = 0

    fun handlePackets(
        sentPacket: Packet,
        receivedPacket: Packet,
        sendTime: Long,
        receiveTime: Long,
    ) {
        sentPacketsCountBySize.plusValueOrPut(sentPacket.packetSize)
        receivedPacketsBySize.plusValueOrPut(receivedPacket.packetSize)

        sentPacketCount++
        receivedPacketCount++

        val ping = ((receiveTime - sendTime) / 1000).toInt()

        maxPing = max(ping, maxPing)
        minPing = min(ping, minPing)

        delaysBySize.getOrPut(sentPacket.packetSize) { mutableListOf() }.add(ping)
    }

    fun handlerPacketWithoutAnswer(
        sentPacket: Packet
    ) {
        sentPacketCount++
        sentPacketsCountBySize.plusValueOrPut(sentPacket.packetSize)
    }

    fun getResult(): TestResult = TestResult(
        averagePing = delaysBySize.values.sumOf { it.sum() } / delaysBySize.values.sumOf { it.size }.toFloat(),
        jitter = maxPing - minPing,
        lossPacketCount = sentPacketCount - receivedPacketCount,
        sentPacketsBySize = sentPacketsCountBySize,
        receivedPacketsBySize = receivedPacketsBySize,
        delaysBySize = delaysBySize,
        averageDelaysBySize = delaysBySize.mapValuesTo(linkedMapOf()) { it.value.average().toFloat() }
    )

}