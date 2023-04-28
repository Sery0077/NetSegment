package sery.vlasenko.netsegment.ui.server

sealed class ServerUiState {
    object Loading : ServerUiState()
    class Loaded(val data: String) : ServerUiState()
    class Error(val message: Any?) : ServerUiState()
}