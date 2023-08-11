package api

import java.nio.file.Path
import java.time.LocalDateTime

/**
 * Immutable state of indexing
 * */
data class IndexingStateSnapshot(
    /**
     * Status of indexing.
     * */
    val status: ProgressableStatus,
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
    val totalFilesNumber: Long?,
    /**
     * Moment of starting task
     * */
    val startTime: LocalDateTime,
    /**
     * Moment of finish or cancel task, otherwise it is now
     * */
    val lastWorkingTime: LocalDateTime,
    /**
     * How long task is going already, if it is in progress, it counts till now
     * */
    val totalTime: Long,
    /**
     * Reason why task failed
     * */
    val failReason: Throwable?
)
