package api

import java.nio.file.Path

/**
 * Atomic piece of found token
 * It has 3 parts:
 *  - filePath: path of file, where token is found
 *  - line: line in the file, where token is found, starts from 1
 *  - column: position in line, where starts token, starts from 1
 * */
data class TokenMatch(
    /**
     * File path, where token was found
     */
    val filePath: Path,
    /**
     * 1-based number of line, which contains token
     */
    val line: Long,
    /**
     * 1-based position in line, where starts token in line
     */
    val column: Long
)