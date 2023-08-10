package utils

/**
 * Returns List of pairs of neighbours items
 * */
fun <T> List<T>.paired(): List<Pair<T, T>> = zip(drop(1))
