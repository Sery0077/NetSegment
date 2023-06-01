package sery.vlasenko.netsegment.model.test.tcp

import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.utils.extensions.toByteArray
import java.nio.ByteBuffer

data class TcpPacketData(
    val dataSize: Int,
    val data: ByteArray
) : Packet() {

    override val packetDataSize: Int
        get() = dataSize

    override val packetSize: Int
        get() = 2 + dataSize

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(packetSize)

        buffer.put(dataSize.toByteArray())
        buffer.put(TcpPacketType.DATA.typeByte)
        buffer.put(data)

        return buffer.array()
    }

    companion object Builder : PacketBuilder {

        override fun fromByteArray(byteArray: ByteArray): TcpPacketData =
            TcpPacketData(
                dataSize = byteArray.size,
                data = byteArray.sliceArray(1..byteArray.lastIndex)
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TcpPacketData

        if (dataSize != other.dataSize) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dataSize
        result = 31 * result + data.contentHashCode()
        return result
    }

}