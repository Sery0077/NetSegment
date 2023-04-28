package sery.vlasenko.netsegment

import androidx.annotation.StringRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import sery.vlasenko.netsegment.utils.ResourceProvider

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val str = getString(R.string.socket_closed, 10)
        assert("Сокет на порту 10 закрыт" == str) {
            println("Found $str")
        }
    }

    fun getString(@StringRes id: Int, vararg args: Any): String = ResourceProvider.getString(id, *args)
}