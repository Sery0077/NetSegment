package sery.vlasenko.netsegment.ui.server.clients

sealed class ConnState {
    class ConnAdd(val position: Int): ConnState()
}