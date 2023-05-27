package sery.vlasenko.netsegment.ui.server.connections

sealed class ConnState {
    class ConnAdd(val position: Int) : ConnState()
}