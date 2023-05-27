package sery.vlasenko.netsegment.utils

import sery.vlasenko.netsegment.model.testscripts.TestItem

object Scripts {

    val testScript = listOf(
        TestItem(
            packetType = PacketType.DATA,
            packetCount = 20,
            dataSize = 72,
            timeout = TimeConst.DEFAULT_TIMEOUT,
            delay = 500L
        ),
        TestItem(
            packetType = PacketType.DATA,
            packetCount = 20,
            dataSize = 130,
            timeout = TimeConst.DEFAULT_TIMEOUT,
            delay = 500L
        ),

        TestItem(
            packetType = PacketType.DATA,
            packetCount = 20,
            dataSize = 180,
            timeout = TimeConst.DEFAULT_TIMEOUT,
            delay = 500L
        ),
        TestItem(
            packetType = PacketType.DATA,
            packetCount = 20,
            dataSize = 250,
            timeout = TimeConst.DEFAULT_TIMEOUT,
            delay = 500L
        ),
        TestItem(
            packetType = PacketType.DATA,
            packetCount = 20,
            dataSize = 300,
            timeout = TimeConst.DEFAULT_TIMEOUT,
            delay = 500L
        ),
    )

}