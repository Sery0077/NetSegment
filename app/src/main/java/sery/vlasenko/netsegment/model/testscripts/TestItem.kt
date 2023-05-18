package sery.vlasenko.netsegment.model.testscripts

import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.TimeConst

data class TestItem(
    val packetType: PacketType,
    val packetCount: Int,
    val dataSize: Int,
    val timeout: Int,
    val delay: Long,
)