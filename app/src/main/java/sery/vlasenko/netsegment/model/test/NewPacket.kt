package sery.vlasenko.netsegment.model.test

abstract class NewPacket {
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

    interface Factory {
        val packetDataSize: Int
        val packetSize: Int
        fun fromByteArray(byteArray: ByteArray): NewPacket
    }
}