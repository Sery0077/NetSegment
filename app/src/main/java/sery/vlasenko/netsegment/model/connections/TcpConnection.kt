package sery.vlasenko.netsegment.model.connections

import java.net.Socket

class TcpConnection(socket: Socket, handler: Thread? = null): Connection<Socket>(socket, handler) {

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

    fun setTimeout(t: Int) {
        socket.soTimeout = t
    }

    override fun close() {
        handler?.interrupt()
        handler?.join()
        
        socket.close()
    }
}