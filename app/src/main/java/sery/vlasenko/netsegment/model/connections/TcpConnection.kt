package sery.vlasenko.netsegment.model.connections

import sery.vlasenko.netsegment.utils.TimeConst
import sery.vlasenko.netsegment.domain.socket_handlers.PingHandler
import java.net.Socket

class TcpConnection(socket: Socket, handler: PingHandler?): Connection<Socket>(socket, handler) {

    companion object {
        const val DEFAULT_TIMEOUT = 1000
    }

    init {
        setTimeout(TimeConst.CLOSE_TIMEOUT)
    }

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

    val input = socket.getInputStream()
    val output = socket.getOutputStream()

    fun setTimeout(t: Int) {
        socket.soTimeout = t
    }

    override fun close() = socket.close()
}