package sery.vlasenko.netsegment.model.test.tcp

import sery.vlasenko.netsegment.model.test.NewPacket
import sery.vlasenko.netsegment.utils.toByteArray
import sery.vlasenko.netsegment.utils.toInt
import java.nio.ByteBuffer

data class TcpPacketData(
    val dataSize: Int,
    val data: ByteArray
) : NewPacket() {

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(2 + dataSize)

        buffer.put(dataSize.toByteArray())
        buffer.put(data)

        return buffer.array()
    }

    companion object : NewPacket.Factory {
        override val packetDataSize: Int
            get() = TODO("Not yet implemented")

        override val packetSize: Int
            get() = TODO("Not yet implemented")

        override fun fromByteArray(byteArray: ByteArray): TcpPacketData =
            TcpPacketData(
                dataSize = byteArray.sliceArray(0..1).toInt(),
                data = byteArray.sliceArray(2..byteArray.lastIndex)
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