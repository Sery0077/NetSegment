package sery.vlasenko.netsegment.ui.server

sealed class SingleEvent {
    class ShowToastEvent(val msg: String): SingleEvent()

    sealed class ConnEvent: SingleEvent() {
        class TestStart(val pos: String): ConnEvent()

        class PingGet(val ping: String): ConnEvent()

    }
}