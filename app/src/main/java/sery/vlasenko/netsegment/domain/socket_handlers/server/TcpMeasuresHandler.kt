package sery.vlasenko.netsegment.domain.socket_handlers.server

import android.os.Looper
import okio.IOException
import sery.vlasenko.netsegment.domain.PacketFactory
import sery.vlasenko.netsegment.domain.TestResultHandler
import sery.vlasenko.netsegment.model.test.TcpPacketType
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketData
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketMeasuresAsk
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketMeasuresEnd
import sery.vlasenko.netsegment.model.test.tcp.TcpPacketMeasuresStart
import sery.vlasenko.netsegment.model.testscripts.TestItem
import sery.vlasenko.netsegment.ui.server.ServerTestCallback
import sery.vlasenko.netsegment.utils.TimeConst.PING_TIMEOUT
import sery.vlasenko.netsegment.utils.toInt
import sery.vlasenko.netsegment.utils.writeAndFlush
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class TcpMeasuresHandler(
    private val socket: Socket,
    private val testScript: List<TestItem>,
    private val callback: (callback: ServerTestCallback) -> Unit,
) : Thread() {

    init {
        isDaemon = true
    }

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
        }
    }

    private fun doMeasures() {
        testScript.forEach { testItem ->
            repeat(testItem.packetCount) {
                val sendPacket = packetFactory.getPacketData(testItem.dataSize)

                try {
                    output.writeAndFlush(sendPacket.send())
                    lastTimePingSend = System.currentTimeMillis()

                    input.read(size)

                    if (size[0].toInt() == -1) {
                        sendCallback(ServerTestCallback.SocketClose)
                        return@forEach
                    }

                    val s = size.toInt()

                    input.read(packetArray, 0, s)

                    val receivedTime = System.currentTimeMillis()

                    sendCallback(ServerTestCallback.PingGet(lastTimePingSend - receivedTime))

                    testResultHandler.handlePackets(
                        sentPacket = sendPacket,
                        receivedPacket = TcpPacketData.fromByteArray(packetArray),
                        sendTime = lastTimePingSend,
                        receiveTime = receivedTime
                    )

                } catch (e: IOException) {
                    sendCallback(ServerTestCallback.SocketClose)
                    return@forEach
                } catch (e: SocketException) {
                    sendCallback(ServerTestCallback.SocketClose)
                    return@forEach
                } catch (e: SocketTimeoutException) {
                    testResultHandler.handlerPacketWithoutAnswer(
                        sentPacket = sendPacket,
                    )
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

                size[0] = input.read().toByte()

                if (size[0].toInt() == -1) {
                    sendCallback(ServerTestCallback.SocketClose)
                    interrupt()
                    break
                }

                size[1] = input.read().toByte()

                val s = size.toInt()

                input.read(packetArray, 0, s)

                if (packetArray[0] == TcpPacketType.MEASURES_ASK.firstByte) {
                    output.write(measuresStart)
                    sendCallback(ServerTestCallback.MeasuresStart)
                    break
                }
            } catch (e: SocketException) {
                return false
            } catch (e: SocketTimeoutException) {
                return false
            }
        }
        return true
    }

    private fun sendCallback(callback: ServerTestCallback) =
        android.os.Handler(Looper.getMainLooper()).post {
            this.callback.invoke(callback)
        }

}