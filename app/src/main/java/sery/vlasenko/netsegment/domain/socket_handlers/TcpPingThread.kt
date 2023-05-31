package sery.vlasenko.netsegment.domain.socket_handlers

import sery.vlasenko.netsegment.model.test.tcp.TcpPacketPing
import sery.vlasenko.netsegment.utils.MyThread
import sery.vlasenko.netsegment.utils.extensions.synchronizedWrite
import java.io.OutputStream
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class TcpPingThread(
    private val output: OutputStream,
    private val onSocketException: () -> Unit,
) : MyThread() {

    val lastTimePingSend = AtomicLong(0)

    private val isPinging = AtomicBoolean(true)

    private var lastTimePing = 0L
    private val ping = TcpPacketPing(isAnswer = false).send()

    override fun run() {
        while (!isInterrupted) {
            if (isPinging.get() && System.currentTimeMillis() - lastTimePing > 200) {
                try {
                    output.synchronizedWrite(ping)

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