package sery.vlasenko.netsegment.domain.socket_handlers.client

import android.os.Looper
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketMeasuresAsk
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketPing
import sery.vlasenko.netsegment.ui.client.ClientHandlerCallback
import sery.vlasenko.netsegment.utils.TimeConst.PING_TIMEOUT
import sery.vlasenko.netsegment.utils.toInt
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicLong

class ClientTcpHandler(
    private val socket: Socket,
    private val callback: (data: ClientHandlerCallback) -> Unit = {},
) : Thread() {

    companion object {
        private val TAG = ClientTcpHandler::class.java.simpleName
    }

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    private val pingAnswer = TcpPacketPing(isAnswer = true).send()
    private val ping = TcpPacketPing(isAnswer = false).send()
    private val measuresAsk = TcpPacketMeasuresAsk().send()

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
                sendCallback(ClientHandlerCallback.SocketClose)
            }
        }
    }.apply {
        isDaemon = true
    }

    private fun handlePing() {
        val ping = System.currentTimeMillis() - lastTimePingSend.get()

        sendCallback(ClientHandlerCallback.PingGet(ping))
    }

    private fun sendCallback(callback: ClientHandlerCallback) =
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }


    private val size = ByteArray(2)
    private val packetArray = ByteArray(1500)

    override fun run() {
        socket.soTimeout = PING_TIMEOUT
        pingThread.start()

        while (!isInterrupted) {
            try {
                size[0] = input.read().toByte()

                if (size[0].toInt() == -1) {
                    sendCallback(ClientHandlerCallback.SocketClose)
                    isPinging = false
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
                    4 -> {
                        when (packetArray[1].toInt()) {
                            1 -> {
                                synchronized(output) {
                                    output.write(measuresAsk)
                                }
                                sendCallback(ClientHandlerCallback.MeasuresStart)
                            }
                            11 -> {
                                sendCallback(ClientHandlerCallback.MeasuresEnd)
                            }
                        }

                    }
                    else -> {
                        synchronized(output) {
                            output.write(size)
                            output.write(packetArray.sliceArray(0 until s))
                            output.flush()
                        }
                    }
                }
            } catch (e: SocketException) {
                interrupt()
                sendCallback(ClientHandlerCallback.SocketClose)
                break
            } catch (e: SocketTimeoutException) {
                sendCallback(ClientHandlerCallback.Timeout)
            }
        }
    }

    override fun interrupt() {
        isPinging = false
        super.interrupt()
    }

    private fun trySleep(s: Long) {
        try {
            sleep(s)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

}