package sery.vlasenko.netsegment.model.test

import java.nio.ByteBuffer

abstract class Packet(
    var time: Long
) {

    companion object {
        @JvmStatic
        protected val headerByte: Byte = 99

        @JvmStatic
        protected val headerSize = 1

        @JvmStatic
        protected val packetTypeSize = 1

        @JvmStatic
        protected val headersSize = packetTypeSize + headerSize
    }

    abstract fun send(): ByteArray

    protected fun addHeader(b: ByteBuffer) {
        b.put(headerByte)
    }

    interface Factory {
        val packetDataSize: Int
        val packetSize: Int
        fun fromByteArray(byteArray: ByteArray): Packet
    }
}