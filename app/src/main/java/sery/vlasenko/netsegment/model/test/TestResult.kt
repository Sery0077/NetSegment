package sery.vlasenko.netsegment.model.test

data class TestResult(
    val averagePing: Int,
    val jitter: Int,
    val sentPacketCount: Int,
    val receivedPacket: Int,
    val lossPacket: Int,
)