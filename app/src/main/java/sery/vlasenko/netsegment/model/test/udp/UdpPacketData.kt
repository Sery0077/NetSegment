package sery.vlasenko.netsegment.model.test.udp

import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketData
import sery.vlasenko.netsegment.utils.extensions.toByteArray
import sery.vlasenko.netsegment.utils.extensions.toInt
import java.nio.ByteBuffer

data class UdpPacketData(
    val dataSize: Int,
    val data: ByteArray
) : Packet() {

    override val packetDataSize: Int
        get() = dataSize

    override val packetSize: Int
        get() = dataSize + 9

    override fun send(): ByteArray {
        val buffer = ByteBuffer.allocate(9 + dataSize)

        buffer.put((3).toByte())
        buffer.put(byteArrayOf(0, 0, 0, 0))
        buffer.put(dataSize.toByteArray())
        buffer.put(byteArrayOf(0, 0))
        buffer.put(data)

        return buffer.array()
    }

    companion object Builder : PacketBuilder {
        val packetHeaderSize: Int = 9

        override fun fromByteArray(byteArray: ByteArray): UdpPacketData =
            UdpPacketData(
                dataSize = byteArray.sliceArray(5..6).toInt(),
                data = byteArray.sliceArray(8..byteArray.lastIndex)
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