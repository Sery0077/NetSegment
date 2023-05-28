package sery.vlasenko.netsegment.utils.extensions

import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException

fun ByteArray.toInt(): Int =
    0 or this[0].toUByte().toInt() or (this[1].toUByte().toInt() shl 8)

fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        this.toByte(),
        (this ushr 8).toByte()
    )
}

fun OutputStream.synchronizedWrite(b: ByteArray) = synchronized(this) {
    writeAndFlush(b)
}

fun OutputStream.writeAndFlush(b: ByteArray) {
    write(b)
    flush()
}

fun DatagramSocket.trySendWithReturnException(dp: DatagramPacket): Exception? {
    try {
        send(dp)
    } catch (e: SocketException) {
        return e
    }
    return null
}
