package sery.vlasenko.netsegment.utils

import java.text.SimpleDateFormat
import java.util.*

fun Long.toTimeFormat(): String {
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("ru"))
    return timeFormatter.format(this)
}