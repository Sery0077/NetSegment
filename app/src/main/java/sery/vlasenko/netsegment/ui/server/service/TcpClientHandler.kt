package sery.vlasenko.netsegment.ui.server.service

import android.net.IpSecManager
import android.net.IpSecManager.UdpEncapsulationSocket
import sery.vlasenko.netsegment.model.test.PacketPing
import sery.vlasenko.netsegment.utils.PacketBuilder
import sery.vlasenko.netsegment.utils.toBytes
import sery.vlasenko.netsegment.utils.toLong
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.*

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
): Thread() {
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