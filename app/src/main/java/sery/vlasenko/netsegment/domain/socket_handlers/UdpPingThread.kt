package sery.vlasenko.netsegment.domain.socket_handlers

import sery.vlasenko.netsegment.model.test.udp.UdpPacketPing
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.datagramPacketFromArray
import java.net.DatagramSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class UdpPingThread(
    private val socket: DatagramSocket,
    private val onSocketException: () -> Unit,
) : MyThread() {

    val lastTimePingSend = AtomicLong(0)

    private val isPinging = AtomicBoolean(true)

    private var lastTimePing = 0L
    private val ping = datagramPacketFromArray(UdpPacketPing().send())

    override fun run() {
        while (!isInterrupted) {
            if (isPinging.get() && System.currentTimeMillis() - lastTimePing > 500) {
                try {
                    socket.send(ping)

                    lastTimePingSend.set(System.nanoTime())
                    lastTimePing = System.currentTimeMillis()
                } catch (e: SocketException) {
                    interrupt()
                    onSocketException.invoke()
                }
            }

            trySleep(50)
        }
    }

    fun stopPing() {
        isPinging.set(false)
    }

    fun startPing() {
        isPinging.set(true)
    }

}