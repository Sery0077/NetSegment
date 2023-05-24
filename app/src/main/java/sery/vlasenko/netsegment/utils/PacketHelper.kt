package sery.vlasenko.netsegment.utils

object PacketHelper {

    fun isPingAnswer(byteArray: ByteArray): Boolean = byteArray[0].toInt() == 2

    fun isAskMeasures(byteArray: ByteArray): Boolean = byteArray[1].toInt() == 1

}