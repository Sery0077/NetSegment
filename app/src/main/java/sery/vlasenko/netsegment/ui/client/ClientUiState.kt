package sery.vlasenko.netsegment.ui.client

sealed class ClientUiState {

    object SocketClosed: ClientUiState()

    object SocketOpened: ClientUiState()

}
