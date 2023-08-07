package sery.vlasenko.netsegment.utils

open class MyThread : Thread() {

    protected fun trySleep(s: Long) {
        try {
            sleep(s)
        } catch (e: InterruptedException) {
            interrupt()
        }
    }

}