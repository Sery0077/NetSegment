package sery.vlasenko.netsegment.domain.socket_handlers.client

import okio.IOException
import sery.vlasenko.netsegment.domain.packet.PacketHandler
import sery.vlasenko.netsegment.model.test.Packet
import sery.vlasenko.netsegment.utils.PacketFactory
import sery.vlasenko.netsegment.utils.PacketType
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class ClientTcpHandler(
    private val socket: Socket,
    var onPacketReceived: (packet: Packet) -> Unit = {},
    var onUnknownPacketType: (packetType: PacketType) -> Unit = {},
    val onClose: () -> Unit = {},
) : Thread() {

    companion object {
        private val TAG = ClientTcpHandler::class.java.simpleName
    }

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    private var isWorking = AtomicBoolean(true)

    init {
        isDaemon = true
    }

    override fun run() {
        socket.soTimeout = 0

        while (isWorking.get()) {
            try {
                val c = input.read()
                val firstByte = input.read()

                if (c == PacketFactory.PACKET_HEADER) {
                    PacketHandler(socket).handlePacket(true, firstByte,
                        onPacketReceived = {
                            onPacketReceived.invoke(it)
                        },
                        onUnknownPacket = {
                            onUnknownPacketType.invoke(it)
                        }
                    )
                } else if (c == -1) {
                    onClose.invoke()
//                    onUnknownPacketType.invoke(PacketType.fromByte(firstByte))
                }
            } catch (e: IOException) {
                println(TAG + e.message)
//                onClose.invoke()
            } catch (e: SocketException) {
                println(TAG + e.message)
            }
        }
    }

    override fun interrupt() {
        isWorking.set(false)
        super.interrupt()
    }

}