package sery.vlasenko.netsegment.utils

import sery.vlasenko.netsegment.model.testscripts.TestItem

object Scripts {

    val testScript = listOf(
        TestItem(
            packetCount = 20,
            dataSize = 70,
        ),
        TestItem(
            packetCount = 20,
            dataSize = 130,
        ),

        TestItem(
            packetCount = 20,
            dataSize = 180,
        ),
        TestItem(
            packetCount = 20,
            dataSize = 250,
        ),
        TestItem(
            packetCount = 20,
            dataSize = 300,
        ),
    )

}