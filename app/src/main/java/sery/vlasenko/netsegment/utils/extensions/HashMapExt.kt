package sery.vlasenko.netsegment.utils

fun <T> HashMap<T, Int>.plusValueOrPut(key: T, i: Int = 1) {
    this[key] = this[key]?.plus(i) ?: i
}

fun <T> HashMap<T, Float>.plusValueOrPut(key: T, i: Float = 1F) {
    this[key] = this[key]?.plus(i) ?: i
}

fun <T, E> MutableMap<T, MutableList<E>>.plusValueOrPut(key: T, l: List<E>) {
    this[key] = this[key]?.apply { addAll(l) } ?: l.toMutableList()
}

fun <T> HashMap<T, Int>.concatInt(map: Map<T, Int>): HashMap<T, Int> {
    map.forEach {
        this.plusValueOrPut(it.key, it.value)
    }
    return this
}

fun <T> HashMap<T, Float>.concatFloat(map: Map<T, Float>): HashMap<T, Float> {
    map.forEach {
        this.plusValueOrPut(it.key, it.value)
    }
    return this
}

fun <T, E> HashMap<T, MutableList<E>>.concat(map: Map<T, List<E>>): HashMap<T, MutableList<E>> {
    map.forEach {
        this.plusValueOrPut(it.key, it.value)
    }
    return this
}