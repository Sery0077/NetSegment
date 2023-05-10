package sery.vlasenko.netsegment.model.connections

abstract class Connection<T>(protected val socket: T) {
    abstract val ip: String?
    abstract val port: Int
    abstract val isConnected: Boolean
    abstract val isClosed: Boolean

    abstract val protocol: Protocol

    abstract fun close()


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Connection<*>

        if (ip != other.ip) return false
        if (port != other.port) return false
        if (isConnected != other.isConnected) return false
        if (protocol != other.protocol) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ip?.hashCode() ?: 0
        result = 31 * result + port
        result = 31 * result + isConnected.hashCode()
        result = 31 * result + protocol.hashCode()
        return result
    }
}

enum class Protocol(name: String) {
    UDP("UDP"),
    TCP("TCP")
}