package utils

/*Returns List of pairs of neighbours items*/
fun <T> List<T>.paired() = zip(drop(1))
