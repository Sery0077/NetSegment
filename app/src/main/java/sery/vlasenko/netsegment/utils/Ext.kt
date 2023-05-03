package sery.vlasenko.netsegment.utils

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