package sery.vlasenko.netsegment.model

import java.util.*

data class LogItem(
    val time: Long = Calendar.getInstance().timeInMillis,
    val message: String,
    val type: LogType = LogType.MESSAGE,
)

enum class LogType {
    MESSAGE,
    ERROR
}