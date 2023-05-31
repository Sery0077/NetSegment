package sery.vlasenko.netsegment.domain.socket_handlers.server.tcp

import android.os.Looper
import okio.IOException
import sery.vlasenko.netsegment.domain.PacketFactory
import sery.vlasenko.netsegment.domain.TestResultHandler
import sery.vlasenko.netsegment.model.test.tcp.*
import sery.vlasenko.netsegment.model.testscripts.TestItem
import sery.vlasenko.netsegment.ui.server.ServerTestCallback
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.Timeouts.PING_TIMEOUT
import sery.vlasenko.netsegment.utils.extensions.toInt
import sery.vlasenko.netsegment.utils.extensions.writeAndFlush
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class ServerTcpMeasuresHandler(
    private val socket: Socket,
    private val testScript: List<TestItem>,
    private val iterationCount: Int,
    private val callback: (callback: ServerTestCallback) -> Unit,
) : MyThread() {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    private val measuresStart = TcpPacketMeasuresStart().send()
    private val measuresEnd = TcpPacketMeasuresEnd().send()
    private val measuresAsk = TcpPacketMeasuresAsk().send()

    private val size = ByteArray(2)
    private val packetArray = ByteArray(1500)

    private val packetFactory: PacketFactory = PacketFactory()

    private val testResultHandler = TestResultHandler()

    private var lastTimePingSend = 0L

    private val startMeasuresTryCount = 5

    override fun run() {
        socket.soTimeout = PING_TIMEOUT

        if (tryStartMeasures()) {
            doMeasures()
        } else {
            sendCallback(ServerTestCallback.MeasuresStartFailed)
            interrupt()
        }
    }

    private fun doMeasures() {
        sendCallback(ServerTestCallback.MeasuresStart)

        repeat(iterationCount) {

            testScript.forEach { testItem ->
                val sendPacket = packetFactory.getTcpPacketData(testItem.dataSize)
                val sendPacketBytes = sendPacket.send()

                repeat(testItem.packetCount) {
                    try {
                        output.writeAndFlush(sendPacketBytes)

                        lastTimePingSend = System.nanoTime()

                        input.read(size)

                        if (size[0].toInt() == -1) {
                            sendCallback(ServerTestCallback.SocketClose)
                            return@forEach
                        }

                        input.read(packetArray, 0, size.toInt())

                        val receivedTime = System.nanoTime()

                        sendCallback(ServerTestCallback.PingGet((lastTimePingSend - receivedTime) / 1000))

                        testResultHandler.handlePackets(
                            sentPacket = sendPacket,
                            receivedPacket = TcpPacketData.fromByteArray(packetArray.sliceArray(0 until size.toInt())),
                            sendTime = lastTimePingSend,
                            receiveTime = receivedTime
                        )

                    } catch (e: IOException) {
                        sendCallback(ServerTestCallback.SocketClose)
                        interrupt()
                        return@forEach
                    } catch (e: SocketException) {
                        sendCallback(ServerTestCallback.SocketClose)
                        interrupt()
                        return@forEach
                    } catch (e: SocketTimeoutException) {
                        testResultHandler.handlerPacketWithoutAnswer(
                            sentPacket = sendPacket,
                        )
                    }
                }
            }
        }

        output.writeAndFlush(measuresEnd)
        sendCallback(ServerTestCallback.MeasuresEnd(testResultHandler.getResult()))
    }


    private fun tryStartMeasures(): Boolean {
        for (i in 1..startMeasuresTryCount) {
            try {
                output.writeAndFlush(measuresAsk)

                input.read(size)

                if (size[0].toInt() == -1) {
                    sendCallback(ServerTestCallback.SocketClose)
                    interrupt()
                    return false
                }

                input.read(packetArray, 0, size.toInt())

                if (packetArray[0] == TcpPacketType.MEASURES_ASK.firstByte) {
                    return true
                }
            } catch (e: SocketException) {
                return false
            } catch (e: SocketTimeoutException) {
                return false
            }
        }
        return false
    }

    private fun sendCallback(callback: ServerTestCallback) =
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }

}