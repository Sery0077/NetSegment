package sery.vlasenko.netsegment.model.test

import java.nio.ByteBuffer

abstract class Packet {

    companion object {
        @JvmStatic
        protected val headerByte: Byte = 99

        @JvmStatic
        protected val HEADER_SIZE = 1

        @JvmStatic
        protected val PACKET_TYPE_SIZE = 1
    }

    abstract fun send(): ByteArray

    protected fun addHeader(b: ByteBuffer) {
        b.put(headerByte)
    }

    interface Factory {
        val arraySize: Int
        fun fromByteArray(byteArray: ByteArray): Packet
    }
}