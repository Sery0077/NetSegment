package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.concat
import sery.vlasenko.netsegment.utils.concatFloat
import sery.vlasenko.netsegment.utils.concatInt
import java.lang.Integer.max
import java.lang.Integer.min

data class TestResult(
    val averagePing: Float,
    val jitter: Int,
    val sentPacketsBySize: LinkedHashMap<Int, Int>,
    val receivedPacketsBySize: LinkedHashMap<Int, Int>,
    val delaysBySize: LinkedHashMap<Int, MutableList<Int>>,
    val averageDelaysBySize: LinkedHashMap<Int, Float>,

    val lossPacketCount: Int = sentPacketsBySize.values.size - receivedPacketsBySize.values.size,
    val packetCount: Int = delaysBySize.values.sumOf { it.size },
    val maxPing: Int = delaysBySize.values.maxOf { it.max() },
    val minPing: Int = delaysBySize.values.minOf { it.min() },
) {
    fun append(res: TestResult) =
        copy(
            averagePing = getPing(res),
            jitter = max(jitter, res.jitter),
            sentPacketsBySize = sentPacketsBySize.concatInt(res.sentPacketsBySize),
            receivedPacketsBySize = receivedPacketsBySize.concatInt(res.receivedPacketsBySize),
            delaysBySize = delaysBySize.concat(res.delaysBySize),
            averageDelaysBySize = averageDelaysBySize.concatFloat(res.averageDelaysBySize),
            lossPacketCount = lossPacketCount + res.lossPacketCount,
            packetCount = packetCount + res.packetCount,
            maxPing = max(res.maxPing, maxPing),
            minPing = min(res.minPing, minPing)
        )

    private fun getPing(res: TestResult): Float {
        return (averagePing * packetCount + res.packetCount * res.averagePing) / (packetCount + res.packetCount)
    }

}