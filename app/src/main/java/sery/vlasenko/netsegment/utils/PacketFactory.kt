package sery.vlasenko.netsegment.utils

import sery.vlasenko.netsegment.model.test.*
import kotlin.random.Random

object PacketFactory {

    const val PACKET_HEADER = 99

    private const val DEFAULT_DATA_SIZE = 50

    fun getPacket(
        packetType: PacketType,
        time: Long? = null,
        dataSize: Int = DEFAULT_DATA_SIZE,
    ): Packet {
        return when (packetType) {
            PacketType.PING -> getPacketPing()
            PacketType.PING_ANSWER -> getPacketPingAnswer(time)
            PacketType.DATA -> getPacketData(dataSize)
            PacketType.SYS -> TODO()
            PacketType.CONNECT -> getPacketConnect()
            PacketType.CONNECT_ANSWER -> getPacketConnectAnswer()
            PacketType.DISCONNECT -> TODO()
            PacketType.SUSPEND -> TODO()
        }
    }

    private fun getPacketConnectAnswer(): Packet {
        return PacketConnectAnswer()
    }

    private fun getPacketConnect(): Packet {
        return PacketConnect()
    }

    fun getPacketPing(): PacketPing = PacketPing()
    fun getPacketPingAnswer(t: Long?): PacketPingAnswer {
        require(t != null) { throw IllegalArgumentException("Time must not be null") }
        return PacketPingAnswer(time = t, isAnswer = true)
    }

    fun getPacketData(dataSize: Int, data: ByteArray = ByteArray(dataSize)): PacketData {
        Random.nextBytes(data)

        return PacketData(
            dataSize = dataSize,
            data = data
        )
    }

//    fun getPacketData(
//        time: Long?,
//        dataSize: Int = 50,
//        data: ByteArray = ByteArray(dataSize)
//    ): PacketData {
//        Random.nextBytes(data)
//
//        return PacketData(
//            time = time,
//            dataSize = dataSize,
//            data = data
//        )
//    }
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