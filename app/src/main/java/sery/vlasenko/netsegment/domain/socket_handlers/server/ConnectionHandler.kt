package sery.vlasenko.netsegment.domain.socket_handlers.server

import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ConnectionHandler(
    private val socket: ServerSocket?,
    private val onConnectionAdd: (socket: Socket) -> Unit,
    private val onClose: () -> Unit = {}
): Thread() {
    init {
        isDaemon = true
    }

    override fun run() {
        while (true) {
            try {
                val socket = socket?.accept()

                if (socket != null) {
                    onConnectionAdd.invoke(socket)
                }
            } catch (e: SocketException) {
                onClose.invoke()
                break
            }
        }
    }

    override fun interrupt() {
        onClose.invoke()
        super.interrupt()
    }

}