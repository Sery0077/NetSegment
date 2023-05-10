package sery.vlasenko.netsegment.model.connections

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

class TcpConnection(socket: Socket): Connection<Socket>(socket) {
    override val ip: String?
        get() = socket.inetAddress.hostAddress

    override val port: Int
        get() = socket.port

    override val isConnected: Boolean
        get() = socket.isConnected

    override val protocol: Protocol
        get() = Protocol.TCP

    override val isClosed: Boolean
        get() = socket.isClosed

    val input: BufferedInputStream = BufferedInputStream(socket.getInputStream())
    val output: BufferedOutputStream = BufferedOutputStream(socket.getOutputStream())

    override fun close() = socket.close()
}