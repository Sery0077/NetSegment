package sery.vlasenko.netsegment.utils

fun String?.orEmpty(): String {
    return if (isNullOrEmpty()) {
        ""
    } else {
        this
    }
}