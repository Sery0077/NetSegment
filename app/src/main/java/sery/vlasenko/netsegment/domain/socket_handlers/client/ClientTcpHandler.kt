package sery.vlasenko.netsegment.domain.socket_handlers.client

import android.os.Looper
import sery.vlasenko.netsegment.domain.socket_handlers.server.tcp.ServerTcpPingThread
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketMeasuresAsk
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketPing
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketType
import sery.vlasenko.netsegment.ui.client.ClientHandlerCallback
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.Timeouts.PING_TIMEOUT
import sery.vlasenko.netsegment.utils.extensions.synchronizedWrite
import sery.vlasenko.netsegment.utils.extensions.toInt
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class ClientTcpHandler(
    private val socket: Socket,
    private val callback: (data: ClientHandlerCallback) -> Unit = {},
) : MyThread() {

    companion object {
        private val TAG = ClientTcpHandler::class.java.simpleName
    }

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    private val pingAnswer = TcpPacketPing(isAnswer = true).send()
    private val measuresAsk = TcpPacketMeasuresAsk().send()

    private val pingThread = ServerTcpPingThread(output) {
        sendCallback(ClientHandlerCallback.SocketClose)
        interrupt()
    }

    private fun handlePing() {
        val ping = System.nanoTime() - pingThread.lastTimePingSend.get()

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
                input.read(size)

                if (size[0].toInt() == -1) {
                    sendCallback(ClientHandlerCallback.SocketClose)
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
                    TcpPacketType.MEASURES.firstByte -> {
                        handleMeasuresPacket(packetArray)
                    }
                    else -> {
                        synchronized(output) {
                            output.write(size)
                            output.write(packetArray.sliceArray(0 until size.toInt()))
                            output.flush()
                        }
                    }
                }
            } catch (e: SocketException) {
                sendCallback(ClientHandlerCallback.SocketClose)
                interrupt()
                break
            } catch (e: SocketTimeoutException) {
                sendCallback(ClientHandlerCallback.Timeout)
            }
        }
    }

    private fun handleMeasuresPacket(packetArray: ByteArray) {
        when (packetArray[1]) {
            TcpPacketType.MEASURES_ASK.secondByte -> {
                pingThread.stopPing()
                output.synchronizedWrite(measuresAsk)
                sendCallback(ClientHandlerCallback.MeasuresStart)
            }
            TcpPacketType.MEASURES_END.secondByte -> {
                pingThread.startPing()
                sendCallback(ClientHandlerCallback.MeasuresEnd)
            }
            TcpPacketType.MEASURES_START.secondByte -> {
                sendCallback(ClientHandlerCallback.MeasuresStart)
            }
        }
    }

    override fun interrupt() {
        pingThread.interrupt()
        super.interrupt()
    }

}