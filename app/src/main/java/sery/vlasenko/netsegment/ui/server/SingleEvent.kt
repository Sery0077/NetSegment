package sery.vlasenko.netsegment.ui.server

sealed class SingleEvent {
    class ShowToastEvent(val msg: String): SingleEvent()
}