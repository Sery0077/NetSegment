package sery.vlasenko.netsegment.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes

@SuppressLint("StaticFieldLeak")
object ResourceProvider {
    lateinit var context: Context

    fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }

    fun getString(@StringRes id: Int, vararg args: Any): String {
        return context.getString(id, *args)
    }
}