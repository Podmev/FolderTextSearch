package api.exception

/**
 * Exception thrown in cases of searching in folder without precalculated index
 */
class NoIndexSearchException(message: String) : SearchException(message)