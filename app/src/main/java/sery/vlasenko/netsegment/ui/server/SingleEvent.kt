package sery.vlasenko.netsegment.ui.server

sealed class SingleEvent {
    class ShowToastEvent(val msg: String) : SingleEvent()

    sealed class ConnEvent : SingleEvent() {

        object TestStart : ConnEvent()

        object TestEnd : ConnEvent()

        class PingGet(val ping: String) : ConnEvent()

        class AddLog(val pos: Int) : ConnEvent()

    }
}