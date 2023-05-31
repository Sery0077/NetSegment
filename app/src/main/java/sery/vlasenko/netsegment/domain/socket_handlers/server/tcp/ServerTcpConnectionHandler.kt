package sery.vlasenko.netsegment.domain.socket_handlers.server.tcp

import sery.vlasenko.netsegment.utils.MyThread
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ServerTcpConnectionHandler(
    private val socket: ServerSocket,
    private val onConnectionAdd: (socket: Socket) -> Unit,
) : MyThread() {

    override fun run() {
        while (!isInterrupted) {
            try {
                val socket = socket.accept()

                if (socket != null) {
                    interrupt()
                    onConnectionAdd.invoke(socket)
                }
            } catch (e: SocketException) {
                interrupt()
            }
        }
    }

}