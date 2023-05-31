package sery.vlasenko.netsegment.model.testscripts

import sery.vlasenko.netsegment.utils.Timeouts.DEFAULT_TIMEOUT

data class TestItem(
    val packetCount: Int,
    val dataSize: Int,
    val timeout: Int = DEFAULT_TIMEOUT,
    val delay: Long = 0,
)