package sery.vlasenko.netsegment.domain.socket_handlers.server

import android.os.Looper
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketPing
import sery.vlasenko.netsegment.ui.server.ServerPingHandlerCallback
import sery.vlasenko.netsegment.utils.TimeConst
import sery.vlasenko.netsegment.utils.toInt
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicLong

class ServerTcpPingHandler(
    private val socket: Socket,
    private val callback: (data: ServerPingHandlerCallback) -> Unit = {},
) : Thread() {

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    companion object {
        private val TAG = ServerTcpPingHandler::class.java.simpleName
    }

    init {
        isDaemon = true
    }

    private val size = ByteArray(2)
    private val packetArray = ByteArray(1500)

    private val pingAnswer = TcpPacketPing(isAnswer = true).send()
    private val ping = TcpPacketPing(isAnswer = false).send()

    @Volatile
    private var isPinging = true

    @Volatile
    var lastTimePingSend = AtomicLong(0)

    private val pingInterval = 100L

    private val pingThread = Thread {
        while (isPinging) {
            try {
                synchronized(output) {
                    output.write(ping)
                    lastTimePingSend.set(System.currentTimeMillis())
                }

                trySleep(pingInterval)
            } catch (e: SocketException) {
                sendCallback(ServerPingHandlerCallback.ConnectionClose)
                interrupt()
            }
        }
    }.apply {
        isDaemon = true
    }

    private fun handlePing() {
        val ping = System.currentTimeMillis() - lastTimePingSend.get()

        sendCallback(ServerPingHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ServerPingHandlerCallback) =
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }

    override fun run() {
        println("fefe ping run")
        socket.soTimeout = TimeConst.PING_TIMEOUT
        pingThread.start()

        while (!isInterrupted) {
            try {
                size[0] = input.read().toByte()

                if (size[0].toInt() == -1) {
                    sendCallback(ServerPingHandlerCallback.ConnectionClose)
                    interrupt()
                    break
                }

                size[1] = input.read().toByte()

                val s = size.toInt()

                input.read(packetArray, 0, s)

                when (packetArray[0].toInt()) {
                    1 -> {
                        synchronized(output) {
                            output.write(pingAnswer)
                        }
                    }
                    2 -> {
                        handlePing()
                    }
                }
            } catch (e: SocketException) {
                sendCallback(ServerPingHandlerCallback.ConnectionClose)
                interrupt()
            } catch (e: SocketTimeoutException) {
                sendCallback(ServerPingHandlerCallback.Timeout)
            }
        }
    }

    private fun trySleep(s: Long) {
        try {
            sleep(s)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

    override fun interrupt() {
        isPinging = false
        super.interrupt()
    }

}