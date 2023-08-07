package impl.trigram

import java.nio.file.Path

/*structure, which contains line from file and index of line (0-based)*/
data class LineInFile(
    val path: Path,
    val lineIndex: Int,
    val line: String
)
