package sery.vlasenko.netsegment.model.test.tcp

enum class TcpPacketType(val firstByte: Byte, val secondByte: Byte) {
    PING(1, -1),
    PING_ANSWER(2, -1),

    MEASURES(4, -1),

    MEASURES_ASK(4, 1),
    MEASURES_START(4, 2),
    MEASURES_END(4, 11),

    DATA(3, -1)
}
