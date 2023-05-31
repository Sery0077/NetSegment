package sery.vlasenko.netsegment.ui.server

sealed class SingleEvent {
    class ShowToastEvent(val msg: String) : SingleEvent()

    sealed class ConnEvent : SingleEvent() {

        class PingGet(val ping: String) : ConnEvent()

        class AddLog(val pos: Int) : ConnEvent()

    }

    sealed class ConnState : SingleEvent() {

        object ConnIdle : ConnState()

        object ConnMeasure : ConnState()

    }
}