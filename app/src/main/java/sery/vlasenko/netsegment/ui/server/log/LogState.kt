package sery.vlasenko.netsegment.ui.server.log

sealed class LogState {
    class LogAdd(val position: Int) : LogState()
}