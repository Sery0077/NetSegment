package sery.vlasenko.netsegment.model.test

abstract class Packet {

    abstract fun send(): ByteArray

    abstract val packetDataSize: Int
    abstract val packetSize: Int

    interface PacketBuilder {
        fun fromByteArray(byteArray: ByteArray): Packet
    }

}