package sery.vlasenko.netsegment.model.testscripts

import sery.vlasenko.netsegment.utils.PacketType

object Scripts {
    val pingScript = listOf<PacketType>(
        PacketType.PING,
        PacketType.PING,
        PacketType.PING,
    )
}