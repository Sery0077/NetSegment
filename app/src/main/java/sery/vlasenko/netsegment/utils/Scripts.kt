package sery.vlasenko.netsegment.utils

import sery.vlasenko.netsegment.model.testscripts.TestItem

object Scripts {

    val testScript = listOf(
        TestItem(
            packetCount = 20,
            dataSize = 70,
            timeout = Timeouts.DEFAULT_TIMEOUT,
            delay = 500L
        ),
        TestItem(
            packetCount = 20,
            dataSize = 130,
            timeout = Timeouts.DEFAULT_TIMEOUT,
            delay = 500L
        ),

        TestItem(
            packetCount = 20,
            dataSize = 180,
            timeout = Timeouts.DEFAULT_TIMEOUT,
            delay = 500L
        ),
        TestItem(
            packetCount = 20,
            dataSize = 250,
            timeout = Timeouts.DEFAULT_TIMEOUT,
            delay = 500L
        ),
        TestItem(
            packetCount = 20,
            dataSize = 300,
            timeout = Timeouts.DEFAULT_TIMEOUT,
            delay = 500L
        ),
    )

}