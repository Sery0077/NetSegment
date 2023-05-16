package sery.vlasenko.netsegment.domain.socket_handlers.server

import okio.IOException
import sery.vlasenko.netsegment.model.test.PacketPing
import sery.vlasenko.netsegment.model.test.PacketPingAnswer
import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.TimeConst
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class PingHandler(
    private val socket: Socket,
    private var input: InputStream = socket.getInputStream(),
    private val output: OutputStream = socket.getOutputStream(),
    var onPingGet: (ping: Long) -> Unit = {},
    var onUnknownPacketType: (packetType: PacketType) -> Unit = {},
    val onClose: () -> Unit = {},
) : Thread() {
    init {
        isDaemon = true
    }

    companion object {
        const val TAG = "CloseHandler"
    }

    val isWorking = AtomicBoolean(true)
    override fun run() {
        socket.soTimeout = TimeConst.PING_TIMEOUT

        synchronized(socket) {
            while (isWorking.get()) {
                try {
                    output.write(PacketBuilder.getPacketPing().send())

                    val count: Int = input.read()

                    when (count) {
                        -1 -> {
                            onClose.invoke()
                            closeSocket()
                            break
                        }
                        PacketBuilder.PACKET_HEADER -> {
                            val packetType = PacketType.fromByte(input.read().toByte())

                            when (packetType) {
                                PacketType.PING_ANSWER -> handlePingAnswerPacket()
                                PacketType.PING -> handlePingPacket()
                                else -> onUnknownPacketType(packetType)
                            }
                        }
                    }

                    trySleep()
                } catch (e: IOException) {
//                    closeSocket()
                    println(TAG + e.message)
                } catch (e: IllegalArgumentException) {
                    println(TAG + e.message)
                }
            }
        }
        println(TAG + "Stopped")
    }
    private fun trySleep() {
        try {
            sleep(TimeConst.PING_DELAY)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

    private fun handlePingPacket() {
        val byteArray = ByteArray(PacketPing.arraySize)

        input.read(byteArray)

        val receivedPacket = PacketPing.fromByteArray(byteArray)
        val ping = Calendar.getInstance().timeInMillis - receivedPacket.time

        onPingGet.invoke(ping)
    }

    private fun handlePingAnswerPacket() {
        val byteArray = ByteArray(PacketPing.arraySize)

        input.read(byteArray)

        val receivedPacket = PacketPingAnswer.fromByteArray(byteArray)
        val ping = Calendar.getInstance().timeInMillis - receivedPacket.time

        println(TAG + ping.toString())

        onPingGet.invoke(ping)
    }

    private fun closeSocket() {
        isWorking.set(false)
        socket.close()
    }

    override fun interrupt() {
        isWorking.set(false)
        super.interrupt()
    }
}