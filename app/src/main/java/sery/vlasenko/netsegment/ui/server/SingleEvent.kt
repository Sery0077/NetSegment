package sery.vlasenko.netsegment.ui.server

import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.LogType

sealed class SingleEvent {
    class ShowToastEvent(val msg: String) : SingleEvent()

    sealed class ConnEvent : SingleEvent() {

        object TestStart : ConnEvent()

        object TestEnd : ConnEvent()

        class PingGet(val ping: Long) : ConnEvent()

        class AddLog(val pos: Int) : ConnEvent()

    }
}