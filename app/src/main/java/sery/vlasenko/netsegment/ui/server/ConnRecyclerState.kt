package sery.vlasenko.netsegment.ui.server

import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.ui.server.connections.ConnectionItem


sealed class ConnRecyclerState {
    class ConnAdd(val position: Int): ConnRecyclerState()

    class ConnRemove(val position: Int): ConnRecyclerState()

    class ConnChanged(val position: Int, val connectionItem: ConnectionItem): ConnRecyclerState()

    object ConnClear: ConnRecyclerState()

    class LogAdd(val position: Int, val logItem: LogItem): ConnRecyclerState()

}