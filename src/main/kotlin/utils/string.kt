package utils

/**
 * Finds all indices of starts of token in line.
 * Works lazily.
 * For empty token returns empty sequence.
 * */
fun String.indicesOf(token: String, ignoreCase: Boolean = false): Sequence<Int> {
    if (token.isEmpty()) {
        return emptySequence()
    }
    fun next(startOffset: Int) = this.indexOf(token, startOffset, ignoreCase).takeIf { it != -1 }
    return generateSequence(next(0)) { prevIndex -> next(prevIndex + 1) }
}