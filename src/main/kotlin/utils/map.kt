package utils

/**
 * Copy of special map, where value is set
 * */
fun <K, V> Map<K, Set<V>>.copy(): Map<K, Set<V>> = buildMap {
    for ((key, valueSet) in this@copy) {
        put(
            key = key,
            value = buildSet {
                for (value in valueSet) add(value)
            }
        )
    }
}