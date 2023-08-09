package api.exception

/**
 * Exception thrown when happens something wrong inside. Normally shouldn't happen.
 */
class RuntimeSearchException(message: String) : SearchException(message)
