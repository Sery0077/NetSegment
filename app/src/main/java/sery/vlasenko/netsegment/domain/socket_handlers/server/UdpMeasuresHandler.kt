package sery.vlasenko.netsegment.domain.socket_handlers.server

import sery.vlasenko.netsegment.ui.server.ServerTestCallback
import java.net.DatagramSocket

class UdpMeasuresHandler(
    val socket: DatagramSocket,
    val callbackHandler: (callback: ServerTestCallback) -> Unit
) : Thread() {


    override fun run() {

    }

}