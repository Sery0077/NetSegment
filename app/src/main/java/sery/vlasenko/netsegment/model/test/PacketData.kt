package sery.vlasenko.netsegment.model.test

import sery.vlasenko.netsegment.utils.PacketFactory
import sery.vlasenko.netsegment.utils.PacketType
import java.nio.ByteBuffer
import java.util.*

class PacketData(
    time: Long = Calendar.getInstance().timeInMillis,
    val dataSize: Int = 50,
    val data: ByteArray,
): Packet(time) {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + PACKET_TYPE_SIZE + TIME_SIZE + Int.SIZE_BYTES + dataSize)
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

        override val arraySize: Int
            get() = PACKET_TYPE_SIZE + TIME_SIZE + Int.SIZE_BYTES

        fun arrayWithDataSize(dataSize: Int): Int = arraySize + dataSize

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

fun main() {
    val s = 10
    val packet1 = PacketFactory.getPacketData(s)

    val packetByte = packet1.send()

    println(packetByte.contentToString())

    val b = ByteBuffer.wrap(packetByte)

//    assert(buffer.get().toInt() == PacketBuilder.PACKET_HEADER) { throw AssertionError() }
//    assert(buffer.get() == PacketType.DATA.toByte()) { throw AssertionError() }
//    assert(buffer.long == packet1.time) { throw AssertionError() }
//
//    val dataSize = buffer.int
//    assert(dataSize == packet1.dataSize) { throw AssertionError() }
//
//    val data = ByteArray(dataSize)
//    buffer.get(data)
//
//    assert(packet1.data.contentEquals(data)) { throw AssertionError() }

    b.get()
    b.get()

    val time = b.long
    val dataSize = b.int

    val data = ByteArray(dataSize)
    b.get(data)

    val buffer = ByteBuffer.allocate(PacketData.arraySize + dataSize)

    buffer.putLong(time)
    buffer.putInt(dataSize)
    buffer.put(data)

    println(buffer.array().contentToString())

    val packet2 = PacketData.fromByteArray(buffer.array())

    println("${packet1.time} ${packet2.time}")
    println("${packet1.dataSize} ${packet2.dataSize}")

    println(packet1.data.contentToString())
    println(packet2.data.contentToString())

//    val packet2 = PacketData.fromByteArray(buffer.array())
}