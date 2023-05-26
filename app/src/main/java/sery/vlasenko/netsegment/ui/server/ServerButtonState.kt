package sery.vlasenko.netsegment.ui.server

sealed class ServerButtonState {

    object SocketOpened: ServerButtonState()

    object SocketClosed: ServerButtonState()

}
