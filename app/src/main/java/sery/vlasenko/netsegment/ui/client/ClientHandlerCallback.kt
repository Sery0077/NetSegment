package sery.vlasenko.netsegment.ui.client

sealed class ClientHandlerCallback {

    class PingGet(val ping: Long): ClientHandlerCallback()

    class Except(val e: Exception): ClientHandlerCallback()

    object SocketClose: ClientHandlerCallback()

    object MeasuresStart: ClientHandlerCallback()

    object MeasuresEnd: ClientHandlerCallback()

    object Timeout: ClientHandlerCallback()

}