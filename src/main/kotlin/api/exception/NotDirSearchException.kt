package api.exception

import java.nio.file.Path

class NotDirSearchException(path: Path): SearchException("Path $path is not directory. Search cannot be performed") {
}