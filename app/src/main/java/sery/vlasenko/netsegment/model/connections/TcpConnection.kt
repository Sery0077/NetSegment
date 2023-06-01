package sery.vlasenko.netsegment.model.connections

import java.net.Socket

class TcpConnection(socket: Socket, handler: Thread) : Connection<Socket>(socket, handler) {

    override val ip: String?
        get() = socket.inetAddress.hostAddress

    override val port: Int
        get() = socket.port

    override val protocol: Protocol
        get() = Protocol.TCP

    override fun close() {
        interruptHandler()
        socket.close()
    }
}