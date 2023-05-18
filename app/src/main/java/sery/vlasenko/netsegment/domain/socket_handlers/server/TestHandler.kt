package sery.vlasenko.netsegment.domain.socket_handlers.server

import sery.vlasenko.netsegment.domain.TestResultHandler
import sery.vlasenko.netsegment.domain.packet.PacketHandler
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.TestResult
import sery.vlasenko.netsegment.model.testscripts.TestItem
import sery.vlasenko.netsegment.utils.PacketFactory
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.TimeConst
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException

class TestHandler(
    private val socket: Socket,
    private val script: List<TestItem>,
    val onPacketReceived: (packet: Packet) -> Unit = {},
    val onUnknownPacketType: (packetType: PacketType) -> Unit = {},
    val onClose: () -> Unit = {},
    val onTestEnd: (testResult: TestResult) -> Unit = {},
) : Thread() {

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    private val packetHandler: PacketHandler = PacketHandler(socket)

    private val testResultHandler = TestResultHandler()

    companion object TestHandler {
        private val TAG = PingHandler::class.java.simpleName
    }

    init {
        isDaemon = true
    }

    override fun run() {
        socket.soTimeout = TimeConst.PING_TIMEOUT

        script.forEach { testItem ->
            socket.soTimeout = testItem.timeout
            repeat(testItem.packetCount) {
                val sentPacket = PacketFactory.getPacket(testItem.packetType)

                try {
                    output.write(sentPacket.send())

                    val c: Int = input.read()
                    val firstByte = input.read()

                    if (c == PacketFactory.PACKET_HEADER) {
                        packetHandler.handlePacket(
                            isNeedToResendPacket = false,
                            firstByte = firstByte,
                            onPacketReceived = { receivedPacket ->
                                testResultHandler.handlePackets(sentPacket, receivedPacket)
                                onPacketReceived.invoke(receivedPacket)
                            },
                            onUnknownPacket = { packetType ->
                                onUnknownPacketType.invoke(packetType)
                            }
                        )
                    } else if (c == -1) {
                        onClose.invoke()
                    }

                } catch (e: SocketTimeoutException) {
                    testResultHandler.handlePackets(sentPacket, null)
                    println(TAG + e.message)
                } catch (e: IOException) {
//                    closeSocket()
                    println(TAG + e.message)
                } catch (e: IllegalArgumentException) {
                    println(TAG + e.message)
                }

                trySleep(testItem.delay)
            }
        }

        onTestEnd.invoke(testResultHandler.getResult())
    }

    private fun trySleep(time: Long) {
        try {
            sleep(time)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

}