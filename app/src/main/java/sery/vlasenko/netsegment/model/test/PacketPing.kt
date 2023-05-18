package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.fromByte
import sery.vlasenko.netsegment.utils.toByte
import java.nio.ByteBuffer
import java.util.*

class PacketPing(
    time: Long = Calendar.getInstance().timeInMillis,
    private val isAnswer: Boolean = false,
) : Packet(time) {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(packetSize)
        addHeader(buffer)

        buffer.put(PacketType.PING.toByte())
        buffer.put(isAnswer.toByte())
        buffer.putLong(time)

        return buffer.array()
    }

    companion object: Factory {
        private const val IS_ANSWER_SIZE = 1
        private const val TIME_SIZE = 8

        override val packetDataSize: Int
            get() = IS_ANSWER_SIZE + TIME_SIZE

        override val packetSize: Int
            get() = headersSize + packetDataSize

        override fun fromByteArray(byteArray: ByteArray): PacketPing {
            val b = ByteBuffer.wrap(byteArray)

            return PacketPing(
                isAnswer = Boolean.Companion.fromByte(b.get()),
                time = b.long,
            )
        }
    }
}