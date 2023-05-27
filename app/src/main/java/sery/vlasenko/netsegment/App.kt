package sery.vlasenko.netsegment

import android.app.Application
import android.os.StrictMode
import sery.vlasenko.netsegment.utils.ResourceProvider

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ResourceProvider.context = this
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }
}