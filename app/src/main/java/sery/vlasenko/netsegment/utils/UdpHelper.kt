package sery.vlasenko.netsegment.utils

import java.net.DatagramPacket

object UdpHelper {
    const val CONNECTION_PACKET_SIZE = 20
    const val DATAGRAM_PACKET_SIZE = 100

    fun udpPacketFromSize(size: Int): DatagramPacket = DatagramPacket(ByteArray(size), size)
}