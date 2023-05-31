package sery.vlasenko.netsegment.model.test.udp

enum class UdpPacketType(val typeByte: Byte, val subTypeByte: Byte) {
    PING(1, -1),
    PING_ANSWER(2, -1),

    CONNECT(5, -1),
    CONNECT_ANSWER(6, -1),

    DISCONNECT(7, -1),

    MEASURES(4, -1),

    MEASURES_ASK(4, 1),
    MEASURES_START(4, 2),
    MEASURES_END(4, 10),

    DATA(3, -1)
}