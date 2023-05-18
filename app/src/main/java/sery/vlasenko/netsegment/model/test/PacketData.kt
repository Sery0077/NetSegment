package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketType
import java.nio.ByteBuffer
import java.util.*

class PacketData(
    time: Long = Calendar.getInstance().timeInMillis,
    val dataSize: Int = 50,
    val data: ByteArray,
): Packet(time) {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(packetSize + dataSize)
        addHeader(buffer)

        buffer.put(PacketType.DATA.toByte())
        buffer.putLong(time)
        buffer.putInt(dataSize)
        buffer.put(data)

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PacketData

        if (time != other.time) return false
        if (dataSize != other.dataSize) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = time.hashCode()
        result = 31 * result + dataSize
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object: Factory {
        private const val TIME_SIZE = 8

        override val packetDataSize: Int
            get() = TIME_SIZE + Int.SIZE_BYTES

        override val packetSize: Int
            get() = headersSize + packetDataSize

        fun arrayWithDataSize(dataSize: Int): Int = packetDataSize + dataSize

        override fun fromByteArray(byteArray: ByteArray): PacketData {
            val b = ByteBuffer.wrap(byteArray)

            val time = b.long
            val dataSize = b.int

            val dataArray = ByteArray(dataSize)
            b.get(dataArray)

            return PacketData(
                time = time,
                dataSize = dataSize,
                data = dataArray
            )
        }
    }

}