package sery.vlasenko.netsegment.domain

import sery.vlasenko.netsegment.model.test.NewPacket
import sery.vlasenko.netsegment.model.test.TestResult

class TestResultHandler {

    private val sentPackets = hashMapOf<Int, NewPacket>()
    private val receivedPackets = hashMapOf<Int, NewPacket>()

    private val delaysByPacketSize = hashMapOf<Int, Long>()

    fun handlePackets(
        sentPacket: NewPacket,
        receivedPacket: NewPacket,
        sendTime: Long,
        receiveTime: Long,
    ) {
        sentPackets.plus(sentPacket.packetSize to sentPacket)
        delaysByPacketSize.plus((receiveTime - sendTime).toInt() to sentPacket)

        receivedPackets.plus(receivedPacket.packetSize to receivedPacket)
    }

    fun handlerPacketWithoutAnswer(
        sentPacket: NewPacket
    ) {
        sentPackets.plus(sentPacket.packetSize to sentPacket)
    }

    fun getResult(): TestResult = TestResult(
        averagePing = delaysByPacketSize.maxOf { it.key } / delaysByPacketSize.size,
        jitter = delaysByPacketSize.maxOf { it.key } - delaysByPacketSize.minOf { it.key },
        sentPacketCount = sentPackets.size,
        receivedPacket = receivedPackets.size,
        lossPacket = sentPackets.size - receivedPackets.size
    )

}