package api

import java.nio.file.Path

/**
 * Immutable state of indexing
 * */
data class IndexingStateSnapshot(
    /**
     * Flag - indexing is finished or no.
     * */
    val finished: Boolean,
    /**
     * Progress of index: can be from 0.0 till 1.0, including both.
     * */
    val progress: Double,
    /**
     * List of file paths visited since last time and saved in buffer.
     * */
    val visitedPathsBuffer: List<Path>,
    /**
     * List of file paths indexed since last time and saved in buffer.
     * */
    val indexedPathsBuffer: List<Path>,
    /**
     * Number of visited files.
     * */
    val visitedFilesNumber: Long,
    /**
     * Number of indexed files.
     * */
    val indexedFilesNumber: Long,
    /**
     * Number of total files in folder, can be null, if it is not calculated yet.
     * */
    val totalFilesNumber: Long?
)
