package sery.vlasenko.netsegment.model.test

abstract class Packet {

    abstract val arraySize: Int

    abstract fun send(): ByteArray
}