package api

import java.nio.file.Path

/**
 * Immutable state of searching.
 * */
data class SearchingStateSnapshot(
    /**
     * Flag - searching is finished or no.
     */
    val finished: Boolean,
    /**
     * Progress of searching: can be from 0.0 till 1.0, including both.
     */
    val progress: Double,
    /**
     * List of file paths visited since last time and saved in buffer.
     */
    val visitedPathsBuffer: List<Path>,
    /**
     * List of received token matches since last time and saved in buffer.
     */
    val tokenMatchesBuffer: List<TokenMatch>,
    /**
     * Number of visited files.
     */
    val visitedFilesNumber: Long,
    /**
     * Number of total files in folder, can be null, if it is not calculated yet.
     */
    val totalFilesNumber: Long?,
    /**
     * Size of visited files in bytes.
     */
    val visitedFilesByteSize: Long,
    /**
     * Size of parsed files in bytes.
     */
    val parsedFilesByteSize: Long,
    /**
     * Size of total files in bytes in folder, can be null, if it is not calculated yet.
     */
    val totalFilesByteSize: Long?

)
