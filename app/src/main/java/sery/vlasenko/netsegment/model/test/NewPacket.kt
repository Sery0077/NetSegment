package sery.vlasenko.netsegment.model.test

abstract class NewPacket {
    companion object {
        @JvmStatic
        protected val headerSize = 1

        @JvmStatic
        protected val packetTypeSize = 1

        @JvmStatic
        protected val headersSize = packetTypeSize + headerSize
    }

    abstract fun send(): ByteArray

    abstract val packetDataSize: Int
    abstract val packetSize: Int

    interface PacketBuilder {
        fun fromByteArray(byteArray: ByteArray): NewPacket
    }

}