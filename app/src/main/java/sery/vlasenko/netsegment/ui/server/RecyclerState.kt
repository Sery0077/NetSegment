package sery.vlasenko.netsegment.ui.server


sealed class RecyclerState {
    class ConnAdd(val position: Int): RecyclerState()
    class ConnRemove(val position: Int): RecyclerState()

    class LogAdd(val position: Int): RecyclerState()
}