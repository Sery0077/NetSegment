package sery.vlasenko.netsegment.domain.socket_handlers.server

import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class TcpConnectionHandler(
    private val socket: ServerSocket,
    private val onConnectionAdd: (socket: Socket) -> Unit,
    private val onClose: () -> Unit = {}
) : Thread() {
    init {
        isDaemon = true
    }

    override fun run() {
        socket.soTimeout = 50

        while (!isInterrupted) {
            try {
                val socket = socket.accept()

                if (socket != null) {
                    onConnectionAdd.invoke(socket)
                }
            } catch (e: SocketException) {
                onClose.invoke()
                break
            } catch (e: SocketTimeoutException) {

            }
        }
    }

    override fun interrupt() {
        onClose.invoke()
        super.interrupt()
    }

}