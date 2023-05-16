package sery.vlasenko.netsegment.utils

import sery.vlasenko.netsegment.model.test.PacketPing

object PacketBuilder {

    const val PACKET_HEADER = 99
    fun getPacketPing(): PacketPing = PacketPing()
    fun getPacketPingAnswer(t: Long): PacketPing = PacketPing(time = t, isAnswer = true)


}

enum class PacketType {
    PING,
    PING_ANSWER,
    DATA,
    SYS,
    CONNECT,
    CONNECT_ANSWER,
    DISCONNECT,
    SUSPEND;

    companion object {
        fun fromByte(b: Byte): PacketType =
            when (b.toInt()) {
                1 -> PING
                2 -> PING_ANSWER
                3 -> DATA
                4 -> SYS
                5 -> CONNECT
                6 -> CONNECT_ANSWER
                7 -> DISCONNECT
                8 -> SUSPEND
                else -> throw IllegalArgumentException("Unexpected argument $b")
            }

        fun fromByte(b: Int): PacketType =
            when (b) {
                1 -> PING
                2 -> PING_ANSWER
                3 -> DATA
                4 -> SYS
                5 -> CONNECT
                6 -> CONNECT_ANSWER
                7 -> DISCONNECT
                8 -> SUSPEND
                else -> throw IllegalArgumentException("Unexpected argument $b")
            }
    }

    fun toByte(): Byte =
        when (this) {
            PING -> 1
            PING_ANSWER -> 2
            DATA -> 3
            SYS -> 4
            CONNECT -> 5
            CONNECT_ANSWER -> 6
            DISCONNECT -> 7
            SUSPEND -> 8
        }
}