package sery.vlasenko.netsegment.utils

import java.net.DatagramPacket
import java.net.InetSocketAddress

fun datagramPacketFromArray(array: ByteArray, addr: InetSocketAddress? = null): DatagramPacket =
    if (addr != null) DatagramPacket(array, array.size, addr) else DatagramPacket(array, array.size)


fun datagramPacketFromSize(size: Int, addr: InetSocketAddress? = null): DatagramPacket =
    if (addr != null) DatagramPacket(
        ByteArray(size),
        size,
        addr
    ) else DatagramPacket(ByteArray(size), size)


fun DatagramPacket.append(dp: DatagramPacket) =
    DatagramPacket(byteArrayOf(*this.data, *dp.data), this.data.size + dp.data.size)