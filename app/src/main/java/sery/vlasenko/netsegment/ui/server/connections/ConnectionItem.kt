package sery.vlasenko.netsegment.ui.server.connections

import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol

data class ConnectionItem(
    val protocol: Protocol,
    val ip: String,
    val port: String,
    val ping: Long = -1L,
    val state: ConnectionItemState = ConnectionItemState.IDLE,
    val logs: MutableList<LogItem> = mutableListOf(),
    val isResultAvailable: Boolean = false,
) {

    fun copyStartTest(): ConnectionItem = this.copy(state = ConnectionItemState.TESTING)
    fun copyStopTest(): ConnectionItem = this.copy(state = ConnectionItemState.IDLE)
    fun copyStopTestWithResult(): ConnectionItem = this.copy(state = ConnectionItemState.IDLE, isResultAvailable = true)
    fun copyResultAvailable(): ConnectionItem = this.copy(isResultAvailable = true)
    fun copyPingUpdate(ping: Long): ConnectionItem = this.copy(ping = ping)

    companion object {
        fun of(conn: Connection<*>): ConnectionItem {
            return ConnectionItem(
                protocol = conn.protocol,
                ip = conn.ip ?: "",
                port = conn.port.toString(),
                ping = -1,
                state = ConnectionItemState.IDLE,
                logs = mutableListOf(),
                isResultAvailable = false
            )
        }
    }
}

enum class ConnectionItemState {
    IDLE,
    TESTING,
}

