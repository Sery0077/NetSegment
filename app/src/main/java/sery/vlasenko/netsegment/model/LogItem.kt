package sery.vlasenko.netsegment.model

import java.util.Calendar

data class LogItem(
    val time: String = Calendar.getInstance().timeInMillis.toString(),
    val message: String,
)