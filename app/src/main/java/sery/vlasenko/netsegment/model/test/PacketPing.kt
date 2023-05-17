package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.fromByte
import sery.vlasenko.netsegment.utils.toByte
import java.nio.ByteBuffer
import java.util.*

data class PacketPing(
    val time: Long = Calendar.getInstance().timeInMillis,
    private val isAnswer: Boolean = false,
) : Packet() {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + PACKET_TYPE_SIZE + IS_ANSWER_SIZE + TIME_SIZE)
        addHeader(buffer)

        buffer.put(PacketType.PING.toByte())
        buffer.put(isAnswer.toByte())
        buffer.putLong(time)

        return buffer.array()
    }

    companion object: Factory {
        private const val IS_ANSWER_SIZE = 1
        private const val TIME_SIZE = 8

        override val arraySize: Int
            get() = PACKET_TYPE_SIZE + IS_ANSWER_SIZE + TIME_SIZE

        override fun fromByteArray(byteArray: ByteArray): PacketPing {
            val b = ByteBuffer.wrap(byteArray)

            return PacketPing(
                isAnswer = Boolean.Companion.fromByte(b.get()),
                time = b.long,
            )
        }
    }
}