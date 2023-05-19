package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketType
import java.nio.ByteBuffer
import java.util.*

class PacketConnectAnswer(
    time: Long = Calendar.getInstance().timeInMillis,
): Packet(time) {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(packetSize)
        addHeader(buffer)

        buffer.put(PacketType.CONNECT_ANSWER.toByte())
        buffer.putLong(time)

        return buffer.array()
    }

    companion object: Factory {
        private const val TIME_SIZE = 8

        override val packetDataSize: Int
            get() = TIME_SIZE

        override val packetSize: Int
            get() = headersSize + packetDataSize

        override fun fromByteArray(byteArray: ByteArray): PacketConnectAnswer {
            val b = ByteBuffer.wrap(byteArray)

            return PacketConnectAnswer(
                time = b.long,
            )
        }
    }

}