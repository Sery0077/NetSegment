package sery.vlasenko.netsegment.model.connections

import java.io.Closeable

abstract class Connection<T : Closeable>(val socket: T, var handler: Thread? = null) {
    abstract val ip: String?
    abstract val port: Int
    abstract val isConnected: Boolean
    abstract val isClosed: Boolean

    abstract val protocol: Protocol

    var state = ConnectionState.IDLE

    abstract fun close()
}

enum class Protocol(name: String) {
    UDP("UDP"),
    TCP("TCP")
}