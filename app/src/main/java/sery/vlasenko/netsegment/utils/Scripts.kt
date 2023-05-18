package sery.vlasenko.netsegment.utils

import sery.vlasenko.netsegment.model.testscripts.TestItem

object Scripts {
    val pingScript = listOf<PacketType>(
        PacketType.PING,
        PacketType.PING,
        PacketType.PING,
    )

    val testScript = listOf<TestItem>(
        TestItem(
            packetType = PacketType.DATA,
            packetCount = 20,
            dataSize = 50,
            timeout = TimeConst.DEFAULT_TIMEOUT,
            delay = 500L
        ),
    )
}