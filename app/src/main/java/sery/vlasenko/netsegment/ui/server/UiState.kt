package sery.vlasenko.netsegment.ui.server

sealed class UiState {
    object SocketOpened: UiState()
    object SocketClosed: UiState()
}
