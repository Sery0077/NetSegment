package sery.vlasenko.netsegment.domain.socket_handlers.server.udp

import android.os.Looper
import sery.vlasenko.netsegment.domain.PacketFactory
import sery.vlasenko.netsegment.domain.TestResultHandler
import sery.vlasenko.netsegment.model.test.udp.*
import sery.vlasenko.netsegment.model.testscripts.TestItem
import sery.vlasenko.netsegment.ui.server.ServerTestCallback
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.Timeouts
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import sery.vlasenko.netsegment.utils.datagramPacketFromSize
import sery.vlasenko.netsegment.utils.extensions.trySendWithReturnException
import java.io.IOException
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException

class ServerUdpMeasuresHandler(
    private val socket: DatagramSocket,
    private val testScript: List<TestItem>,
    private val iterationCount: Int,
    private val callback: (callback: ServerTestCallback) -> Unit
) : MyThread() {

    init {
        isDaemon = true
    }

    private val measuresStart = datagramPacketFromArray(UdpPacketMeasuresStart().send())
    private val measuresEnd = datagramPacketFromArray(UdpPacketMeasuresEnd().send())
    private val measuresAsk = datagramPacketFromArray(UdpPacketMeasuresAsk().send())

    private val buf = datagramPacketFromSize(1500)

    private val packetFactory: PacketFactory = PacketFactory()

    private val testResultHandler = TestResultHandler()

    private var lastTimePingSend = 0L

    private val startMeasuresTryCount = 5


    override fun run() {
        socket.soTimeout = Timeouts.PING_TIMEOUT

        if (tryStartMeasures()) {
            doMeasures()
        } else {
            sendCallback(ServerTestCallback.MeasuresStartFailed)
        }
    }

    private fun doMeasures() {
        sendCallback(ServerTestCallback.MeasuresStart)

        if (socket.trySendWithReturnException(measuresStart) != null) {
            sendCallback(ServerTestCallback.SocketClose)
            return
        }

        repeat(iterationCount) {
            testScript.forEach { testItem ->
                val sendPacket = packetFactory.getUdpPacketData(testItem.dataSize)
                val datagramPacket = datagramPacketFromArray(sendPacket.send())

                repeat(testItem.packetCount) {
                    try {
                        socket.send(datagramPacket)

                        lastTimePingSend = System.nanoTime()

                        socket.receive(buf)

                        val receivedTime = System.nanoTime()

                        if (buf.data[0] == UdpPacketType.DISCONNECT.typeByte) {
                            sendCallback(ServerTestCallback.SocketClose)
                            return
                        }

                        sendCallback(ServerTestCallback.PingGet(receivedTime - lastTimePingSend))

                        testResultHandler.handlePackets(
                            sentPacket = sendPacket,
                            receivedPacket = UdpPacketData.fromByteArray(buf.data.sliceArray(0 until buf.length)),
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
        }

        if (socket.trySendWithReturnException(measuresEnd) != null) {
            sendCallback(ServerTestCallback.SocketClose)
            return
        }

        sendCallback(ServerTestCallback.MeasuresEnd(testResultHandler.getResult()))
    }

    private fun tryStartMeasures(): Boolean {
        for (i in 1..startMeasuresTryCount) {
            try {
                socket.send(measuresAsk)

                socket.receive(buf)

                if (buf.data[0] == UdpPacketType.DISCONNECT.typeByte) {
                    sendCallback(ServerTestCallback.SocketClose)
                    interrupt()
                    return false
                } else if (
                    buf.data[0] == UdpPacketType.MEASURES.typeByte
                    && buf.data[5] == UdpPacketType.MEASURES_ASK.subTypeByte
                ) {
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