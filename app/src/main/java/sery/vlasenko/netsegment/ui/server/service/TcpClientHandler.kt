package sery.vlasenko.netsegment.ui.server.service

import sery.vlasenko.netsegment.utils.toBytes
import sery.vlasenko.netsegment.utils.toLong
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class TcpClientHandler(
    private val dataInputStream: DataInputStream,
    private val dataOutputStream: DataOutputStream
) : Thread() {
    override fun run() {
        val data = ByteArray(Long.SIZE_BYTES)
        while (true) {
            try {
                val count: Int = dataInputStream.read(data, 0, Long.SIZE_BYTES)
                if (count > 0) {
                    println("recevied ${data.toLong()}")
                    dataOutputStream.write(Calendar.getInstance().timeInMillis.toBytes())
                } else if (count == -1) {
                    println("socket is closed")
                    break
                }
            } catch (e: IOException) {
                System.err.println(e.message)
            }
        }
        println("ConnectionWorker stoped")
    }

    companion object {
        private val TAG = TcpClientHandler::class.java.simpleName
    }
}