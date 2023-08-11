package api

import java.nio.file.Path
import java.util.concurrent.Future

/**
 * State which api gives when you make index at folder.
 * It can help to control process of indexing, since it can be long process.
 * */
interface IndexingState : ProgressableState {
    /**
     * Result - all file paths in directory recursively, which were indexed
     * */
    val result: Future<List<Path>>

    /**
     * Get the newfound portion of file paths visited after previous call.
     * You can set flush true, if you don't want to save current buffer value for next time.
     * Otherwise, buffer will grow till the end, and it will be equals result.
     * */
    fun getVisitedPathsBuffer(flush: Boolean): List<Path>

    /**
     * Get the newfound portion of file paths analyzed (indexed) after previous call.
     * You can set flush true, if you don't want to save current buffer value for next time.
     * Otherwise, buffer will grow till the end, and it will be equals result.
     * */
    fun getIndexedPathsBuffer(flush: Boolean): List<Path>

    /**
     * Number of visited files during indexing.
     * Value updates all the time. After successful finishing should be equal totalFiles
     * */
    val visitedFilesNumber: Long

    /**
     * Number of indexed files during indexing.
     * Value updates all the time. After successful finishing should be equal totalFiles
     * */
    val indexedFilesNumber: Long

    /**
     * Total number of files in folder.
     * Value updates once after finishing walking all files.
     * At first, it is null.
     * */
    val totalFilesNumber: Long?
}

/**
 * Takes immutable snapshot of IndexingState
 * */
fun IndexingState.toSnapshot(): IndexingStateSnapshot =
    IndexingStateSnapshot(
        status = status,
        progress = progress,
        visitedPathsBuffer = getVisitedPathsBuffer(true),
        indexedPathsBuffer = getIndexedPathsBuffer(true),
        visitedFilesNumber = visitedFilesNumber,
        indexedFilesNumber = indexedFilesNumber,
        totalFilesNumber = totalFilesNumber,
        startTime = startTime,
        lastWorkingTime = lastWorkingTime,
        totalTime = totalTime,
        failReason = failReason
    )