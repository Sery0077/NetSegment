package sery.vlasenko.netsegment.model.testscripts

data class TestItem(
    val packetCount: Int,
    val dataSize: Int,
    val timeout: Int,
    val delay: Long,
)