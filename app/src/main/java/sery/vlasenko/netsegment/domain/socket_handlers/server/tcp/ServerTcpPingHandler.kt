package sery.vlasenko.netsegment.domain.socket_handlers.server.tcp

import android.os.Looper
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketPing
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketType
import sery.vlasenko.netsegment.ui.server.ServerPingHandlerCallback
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.Timeouts
import sery.vlasenko.netsegment.utils.extensions.synchronizedWrite
import sery.vlasenko.netsegment.utils.extensions.toInt
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class ServerTcpPingHandler(
    private val socket: Socket,
    private val callback: (data: ServerPingHandlerCallback) -> Unit = {},
) : MyThread() {

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    companion object {
        private val TAG = ServerTcpPingHandler::class.java.simpleName
    }

    private val size = ByteArray(2)
    private val packetArray = ByteArray(1500)

    private val pingAnswer = TcpPacketPing(isAnswer = true).send()

    private val pingThread = ServerTcpPingThread(output) {
        sendCallback(ServerPingHandlerCallback.ConnectionClose)
        interrupt()
    }

    private fun handlePing() {
        val ping = System.nanoTime() - pingThread.lastTimePingSend.get()
        sendCallback(ServerPingHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ServerPingHandlerCallback) =
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }

    override fun run() {
        socket.soTimeout = Timeouts.PING_TIMEOUT

        pingThread.start()

        while (!isInterrupted) {
            try {
                input.read(size)

                if (size[0].toInt() == -1) {
                    sendCallback(ServerPingHandlerCallback.ConnectionClose)
                    interrupt()
                    break
                }

                input.read(packetArray, 0, size.toInt())

                when (packetArray[0]) {
                    TcpPacketType.PING.firstByte -> {
                        output.synchronizedWrite(pingAnswer)
                    }
                    TcpPacketType.PING_ANSWER.firstByte -> {
                        handlePing()
                    }
                }
            } catch (e: SocketException) {
                sendCallback(ServerPingHandlerCallback.ConnectionClose)
                interrupt()
                break
            } catch (e: SocketTimeoutException) {
                sendCallback(ServerPingHandlerCallback.Timeout)
            }
        }
    }

    override fun interrupt() {
        pingThread.interrupt()
        super.interrupt()
    }

}