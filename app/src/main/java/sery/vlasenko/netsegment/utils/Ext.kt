package sery.vlasenko.netsegment.utils

import java.net.DatagramPacket
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
