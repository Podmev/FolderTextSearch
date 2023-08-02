package api

import java.nio.file.Path

/*Atomic piece of found token
* It has 3 parts:
*  - filePath: path of file, where token is found
*  - line: line in the file, where token is found
*  - column: position in line, where starts token
* */
data class TokenMatch(val filePath: Path, val line: Long, val column: Long)