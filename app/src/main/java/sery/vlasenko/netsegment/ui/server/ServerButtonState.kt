package sery.vlasenko.netsegment.ui.server

sealed class ServerButtonState {

    object TcpSocketOpened: ServerButtonState()

    object TcpSocketClosed: ServerButtonState()

    object UdpSocketOpened: ServerButtonState()

    object UdpSocketClosed: ServerButtonState()

}
