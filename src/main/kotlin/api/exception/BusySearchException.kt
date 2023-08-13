package api.exception

/**
 * Exception thrown in cases of concurrent indexing folders
 */
class BusySearchException(message: String) : SearchException(message)