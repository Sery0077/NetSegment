package sery.vlasenko.netsegment.model.connections

import java.io.Closeable

abstract class Connection<T : Closeable>(val socket: T, protected var handler: Thread) {
    abstract val ip: String?
    abstract val port: Int

    abstract val protocol: Protocol

    var state = ConnectionState.IDLE

    abstract fun close()

    fun interruptHandler() {
        this.handler.interrupt()
        this.handler.join()
    }

    fun setAndStartNewHandler(handler: Thread) {
        this.handler.interrupt()
        this.handler.join()

        this.handler = handler.apply { start() }
    }
}

enum class Protocol(name: String) {
    UDP("UDP"),
    TCP("TCP")
}