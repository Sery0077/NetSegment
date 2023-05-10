package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.fromByte
import sery.vlasenko.netsegment.utils.toByte
import java.nio.ByteBuffer
import java.util.*

data class PacketPing(
    private val type: PacketType = PacketType.PING,
    val time: Long = Calendar.getInstance().timeInMillis,
    val isAnswer: Boolean = false,
) : Packet() {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(arraySize)
        addHeader(buffer)

        buffer.put(type.toByte())
        buffer.put(isAnswer.toByte())
        buffer.putLong(time)

        return buffer.array()
    }

    companion object: PacketBuilder {
        override val arraySize: Int
            get() = 11

        override fun fromByteArray(byteArray: ByteArray): PacketPing {
            val b = if (byteArray[0] == headerByte)
                ByteBuffer.wrap(byteArray, 1, arraySize)
            else
                ByteBuffer.wrap(byteArray)

            return PacketPing(
                type = PacketType.fromByte(b.get()),
                isAnswer = Boolean.Companion.fromByte(b.get(1)),
                time = b.getLong(2),
            )
        }
    }
}