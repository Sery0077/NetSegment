package sery.vlasenko.netsegment.ui.server

import sery.vlasenko.netsegment.model.LogItem


sealed class RecyclerState {
    class ConnAdd(val position: Int): RecyclerState()
    class ConnRemove(val position: Int): RecyclerState()

    class ConnChanged(val position: Int, val ping: Long): RecyclerState()

    class LogAdd(val position: Int, val logItem: LogItem): RecyclerState()
}