package sery.vlasenko.netsegment.utils

fun <T> LinkedHashMap<T, Int>.plusValueOrPut(key: T, i: Int = 1) {
    this[key] = this[key]?.plus(i) ?: i
}

fun <T> LinkedHashMap<T, Float>.plusValueOrPut(key: T, i: Float = 1F) {
    this[key] = this[key]?.plus(i) ?: i
}

fun <T, E> MutableMap<T, MutableList<E>>.plusValueOrPut(key: T, l: List<E>) {
    this[key] = this[key]?.apply { addAll(l) } ?: l.toMutableList()
}

fun <T> LinkedHashMap<T, Int>.concatInt(map: Map<T, Int>): LinkedHashMap<T, Int> {
    map.forEach {
        this.plusValueOrPut(it.key, it.value)
    }
    return this
}

fun <T> LinkedHashMap<T, Float>.concatFloat(map: Map<T, Float>): LinkedHashMap<T, Float> {
    map.forEach {
        this.plusValueOrPut(it.key, it.value)
    }
    return this
}

fun <T, E> LinkedHashMap<T, MutableList<E>>.concat(map: Map<T, List<E>>): LinkedHashMap<T, MutableList<E>> {
    map.forEach {
        this.plusValueOrPut(it.key, it.value)
    }
    return this
}