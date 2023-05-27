package sery.vlasenko.netsegment.ui.server

import sery.vlasenko.netsegment.model.test.TestResult

sealed class ServerTestCallback {

    object SocketClose : ServerTestCallback()

    object MeasuresStart : ServerTestCallback()

    object MeasuresStartFailed : ServerTestCallback()

    class PingGet(val ping: Long) : ServerTestCallback()

    class MeasuresEnd(val result: TestResult) : ServerTestCallback()

}
