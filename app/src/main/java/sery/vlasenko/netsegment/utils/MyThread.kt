package sery.vlasenko.netsegment.utils

open class MyThread: Thread() {

    init {
        isDaemon = true
    }

    protected fun trySleep(s: Long) {
        try {
            sleep(s)
        } catch (e: InterruptedException) {
            interrupt()
        }
    }

}