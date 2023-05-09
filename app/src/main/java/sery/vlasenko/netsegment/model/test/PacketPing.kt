package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.fromByte
import sery.vlasenko.netsegment.utils.toByte
import java.nio.ByteBuffer
import java.util.Calendar

data class PacketPing(
    private val type: PacketType = PacketType.PING,
    val time: Long = Calendar.getInstance().timeInMillis,
    val isAnswer: Boolean = false,
) : Packet() {

    override val arraySize: Int = 11

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(arraySize)
        buffer.put(99)
        buffer.put(type.toByte())
        buffer.put(isAnswer.toByte())
        buffer.putLong(time)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray): PacketPing {
            val b = ByteBuffer.wrap(byteArray)

            return PacketPing(
                type = PacketType.fromByte(b.get()),
                isAnswer = Boolean.Companion.fromByte(b.get(1)),
                time = b.getLong(2),
            )
        }
    }
}

fun main() {
    val p = PacketBuilder.getPacketPing()

    val b = p.send()
    val p2 = PacketPing.fromByteArray(b)
    println()
}