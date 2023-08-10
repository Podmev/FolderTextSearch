package api.exception

import java.nio.file.Path

/**
 * Exception thrown when there is no folder by path to search
 * */
class NotDirSearchException(path: Path) : SearchException("Path $path is not directory. Search cannot be performed")