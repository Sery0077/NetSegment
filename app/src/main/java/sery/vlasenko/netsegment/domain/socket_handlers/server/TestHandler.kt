package sery.vlasenko.netsegment.domain.socket_handlers.server

import sery.vlasenko.netsegment.domain.packet.PacketHandler
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.model.test.PacketData
import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.PacketType
import sery.vlasenko.netsegment.utils.TimeConst
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class TestHandler(
    private val socket: Socket,
    var onPacketReceived: (packet: Packet) -> Unit = {},
    var onUnknownPacketType: (packetType: PacketType) -> Unit = {},
    val onClose: () -> Unit = {},
) : Thread() {

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    companion object TestHandler {
        private val TAG = PingHandler::class.java.simpleName
    }

    init {
        isDaemon = true
    }

    val isWorking = AtomicBoolean(true)

    override fun run() {
        socket.soTimeout = TimeConst.PING_TIMEOUT

        while (isWorking.get()) {
            try {
                output.write(PacketBuilder.getPacketData(1000).send())

                val c: Int = input.read()
                val firstByte = input.read()

                if (c == PacketBuilder.PACKET_HEADER) {
                    PacketHandler(socket).handlePacket(false, firstByte,
                        onPacketReceived = { packet ->
                            (packet as? PacketData)?.let {
                                println("fefe ping = ${Calendar.getInstance().timeInMillis - it.time}")
                                onPacketReceived.invoke(it)
                            }
                        },
                        onUnknownPacket = {
                            onUnknownPacketType.invoke(it)
                        }
                    )
                } else if (c == -1) {
                    isWorking.set(false)
                    onClose.invoke()
                }

            } catch (e: IOException) {
//                    closeSocket()
                println(TAG + e.message)
            } catch (e: IllegalArgumentException) {
                println(TAG + e.message)
            }

            trySleep()
        }
    }

    private fun trySleep() {
        try {
            sleep(TimeConst.PING_DELAY)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

    override fun interrupt() {
        isWorking.set(false)
        super.interrupt()
    }

}