package sery.vlasenko.netsegment.ui.client

sealed class ClientUiState {

    object Connected: ClientUiState()

    object Disconnected: ClientUiState()

    object Connecting: ClientUiState()

}
