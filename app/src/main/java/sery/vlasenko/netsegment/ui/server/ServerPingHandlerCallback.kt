package sery.vlasenko.netsegment.ui.server

sealed class ServerPingHandlerCallback {

    class PingGet(val ping: Long) : ServerPingHandlerCallback()

    object Timeout : ServerPingHandlerCallback()

    object ConnectionClose : ServerPingHandlerCallback()

}