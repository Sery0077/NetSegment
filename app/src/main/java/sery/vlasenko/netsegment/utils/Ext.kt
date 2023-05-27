package sery.vlasenko.netsegment.utils

import java.io.OutputStream
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.nio.ByteBuffer


fun Long.toBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.putLong(this)
    return buffer.array()
}

fun ByteArray.toLong(): Long {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.put(this)
    buffer.flip()
    return buffer.long
}

fun ByteArray.toInt(): Int =
    0 or (this[0].toUByte().toInt() shl 8 * 0) or (this[1].toUByte().toInt() shl 8 * 1)

fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        this.toByte(),
        (this ushr 8).toByte()
    )
}

fun OutputStream.writeAndFlush(b: ByteArray) {
    write(b)
    flush()
}

fun datagramPacketFromArray(array: ByteArray, addr: InetSocketAddress? = null): DatagramPacket =
    if (addr != null) DatagramPacket(array, array.size, addr) else DatagramPacket(array, array.size)

fun datagramPacketFromSize(size: Int, addr: InetSocketAddress? = null): DatagramPacket =
    if (addr != null) DatagramPacket(
        ByteArray(size),
        size,
        addr
    ) else DatagramPacket(ByteArray(size), size)

fun DatagramPacket.append(dp: DatagramPacket) =
    DatagramPacket(byteArrayOf(*this.data, *dp.data), this.data.size + dp.data.size)

fun Boolean.Companion.fromByte(b: Byte): Boolean =
    when (b.toInt()) {
        1 -> true
        0 -> false
        else -> throw IllegalArgumentException("Unexpected argument $b")
    }

fun Boolean.toByte(): Byte =
    when (this) {
        true -> 1
        false -> 0
    }
