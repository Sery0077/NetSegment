package sery.vlasenko.netsegment

import android.app.Application
import sery.vlasenko.netsegment.utils.ResourceProvider

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        ResourceProvider.context = this
    }
}