package sery.vlasenko.netsegment.ui.server

sealed class LogState {
    class LogAdd(val position: Int): LogState()
}