package sery.vlasenko.netsegment.model.testscripts

import sery.vlasenko.netsegment.utils.PacketType

data class TestItem(
    val packetType: PacketType,
    val packetCount: Int = 1,
)