package impl.trigram

import java.nio.file.Path

/**
 * Structure, which contains line from file and index of line (0-based)
 * */
data class LineInFile(
    /**
     * File path, where located the line
     * */
    val path: Path,
    /**
     * 0-based index of line from file
     * */
    val lineIndex: Int,
    /**
     * Content of line from file
     * */
    val line: String
)
