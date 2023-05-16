package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.fromByte
import sery.vlasenko.netsegment.utils.toByte
import java.nio.ByteBuffer
import java.util.*

data class PacketPing(
    val time: Long = Calendar.getInstance().timeInMillis,
    val isAnswer: Boolean = false,
) : Packet() {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(arraySize + 1)
        addHeader(buffer)

        buffer.put(PacketType.PING.toByte())
        buffer.put(isAnswer.toByte())
        buffer.putLong(time)

        return buffer.array()
    }

    companion object: Factory {
        override val arraySize: Int
            get() = 10

        override fun fromByteArray(byteArray: ByteArray): PacketPing {
            val b = ByteBuffer.wrap(byteArray)

            return PacketPing(
                isAnswer = Boolean.Companion.fromByte(b.get(0)),
                time = b.getLong(1),
            )
        }
    }
}

data class PacketPingAnswer(
    val time: Long = Calendar.getInstance().timeInMillis,
    val isAnswer: Boolean = true,
) : Packet() {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(arraySize)
        addHeader(buffer)

        buffer.put(PacketType.PING_ANSWER.toByte())
        buffer.put(isAnswer.toByte())
        buffer.putLong(time)

        return buffer.array()
    }

    companion object: Factory {
        override val arraySize: Int
            get() = 10

        override fun fromByteArray(byteArray: ByteArray): PacketPing {
            val b = ByteBuffer.wrap(byteArray)

            return PacketPing(
                isAnswer = Boolean.Companion.fromByte(b.get(0)),
                time = b.getLong(1),
            )
        }
    }
}