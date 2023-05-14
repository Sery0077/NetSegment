package sery.vlasenko.netsegment.ui.server.service

import okio.IOException
import sery.vlasenko.netsegment.model.test.PacketPing
import sery.vlasenko.netsegment.model.test.TestResult
import sery.vlasenko.netsegment.model.testscripts.Scripts
import sery.vlasenko.netsegment.model.testscripts.Timeouts
import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.PacketType
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class TcpClientHandler(
    private val dataInputStream: DataInputStream,
    private val dataOutputStream: DataOutputStream,
    val close: () -> Unit,
) : Thread() {
    override fun run() {
        while (true) {
            try {
                val count: Int = dataInputStream.read()

                if (count == 99) {
                    val data = ByteArray(10)
                    dataInputStream.read(data, 0, 10)
                    val p = PacketPing.fromByteArray(data)

                    dataOutputStream.write(PacketBuilder.getPacketPingAnswer(p.time).send())

                } else if (count == -1) {
                    println("socket is closed")
                    break
                }

            } catch (e: IOException) {
                System.err.println(e.message)
            }
        }
        println("ConnectionWorker stoped")
        close.invoke()
        interrupt()
    }

    companion object {
        private val TAG = TcpClientHandler::class.java.simpleName
    }
}

class TcpHandler(
    private val dataInputStream: BufferedInputStream,
    private val dataOutputStream: BufferedOutputStream,
    val close: () -> Unit,
) : Thread() {
    override fun run() {
        while (true) {
            try {
                val count: Int = dataInputStream.read()

                if (count == 99) {
                    val data = ByteArray(10)
                    dataInputStream.read(data, 0, 10)
                    val p = PacketPing.fromByteArray(data)

                    println("Received" + p.time)

                    dataOutputStream.write(PacketBuilder.getPacketPingAnswer(p.time).send())

                } else if (count == -1) {
                    println("socket is closed")
                    break
                }

            } catch (e: IOException) {
                System.err.println(e.message)
            }
        }
        println("ConnectionWorker stoped")
        close.invoke()
    }
}

class MyHandler(
    private var socket: Socket,
    private var input: InputStream,
    private val output: OutputStream,
    val onClose: () -> Unit,
): Thread() {

    private var isClosingListener = AtomicBoolean(true)

    private var testResult: TestResult? = null
    override fun run() {
        synchronized(socket) {
            startTest()
        }
    }

    fun startTest(script: List<PacketType> = Scripts.pingScript): TestResult {
        println("Test started" + currentThread().id)
        println("Test started")
        testResult = TestResult()

        isClosingListener.set(false)

        sleep(1000)

        script.forEach {
            if (it == PacketType.PING) {
                handlePingType()
            }
        }

        isClosingListener.set(true)

        println(testResult)

        println("Test stopped")

        return testResult!!
    }

    private fun handlePingType() {
        val p = PacketBuilder.getPacketPing()

        socket.soTimeout = Timeouts.PING_TIMEOUT

        try {
            output.write(p.send())

            val r = input.read()

            if (r == -1) {
                onClose.invoke()
                return
            } else if (r == PacketBuilder.PACKET_HEADER) {
                val byteArray = ByteArray(PacketPing.arraySize - 1)

                input.read(byteArray)

                val receivedPacket = PacketPing.fromByteArray(byteArray)

                testResult?.ping?.add(Calendar.getInstance().timeInMillis - receivedPacket.time)
            }
        } catch (e: SocketTimeoutException) {
            println("HandlePingType " + e.message)
        }

        socket.soTimeout = Timeouts.CLOSE_TIMEOUT
    }
}