package sery.vlasenko.netsegment.model.test

import java.nio.ByteBuffer

abstract class Packet {

    companion object {
        @JvmStatic
        protected val headerByte: Byte = 99
    }

    abstract fun send(): ByteArray

    protected fun addHeader(b: ByteBuffer) {
        b.put(headerByte)
    }

    interface PacketBuilder {
        val arraySize: Int

        fun fromByteArray(byteArray: ByteArray): Packet
    }
}