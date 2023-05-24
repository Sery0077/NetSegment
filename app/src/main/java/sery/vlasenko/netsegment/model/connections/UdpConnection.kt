package sery.vlasenko.netsegment.model.connections

import java.net.DatagramSocket

class UdpConnection(socket: DatagramSocket, handler: Thread?): Connection<DatagramSocket>(socket, handler) {
    override val ip: String
        get() = socket.inetAddress.hostAddress ?: ""

    override val port: Int
        get() = socket.port

    override val isConnected: Boolean
        get() = socket.isConnected

    override val protocol: Protocol
        get() = Protocol.UDP

    override val isClosed: Boolean
        get() = socket.isClosed

    override fun close() {
        socket.close()
    }
}