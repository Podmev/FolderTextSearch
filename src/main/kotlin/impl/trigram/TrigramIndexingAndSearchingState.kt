package impl.trigram

import api.*
import api.exception.FailedSearchException
import utils.WithLogging
import utils.max
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.Future

/**
 * State of trigram search api for searching and indexing.
 * */
class TrigramIndexingAndSearchingState(
    override val indexingState: IndexingState,
    override val searchingState: SearchingState,
) : IndexingAndSearchingState, WithLogging() {
    //cancel
    private var cancellationAction: () -> Unit = {}

    override val startTime: LocalDateTime
        get() = indexingState.startTime
    override val lastWorkingTime: LocalDateTime
        get() = max(indexingState.lastWorkingTime, searchingState.lastWorkingTime)

    override val status: ProgressableStatus
        get() = trigramAggregateIndexAndSearchStatus(indexingState.status, searchingState.status)

    override val failReason: Throwable?
        get() = indexingState.failReason ?: searchingState.failReason
        ?: (if (status == ProgressableStatus.FAILED) FailedSearchException("Failed status") else null)

    override val tokenMatchesNumber: Long
        get() = searchingState.tokenMatchesNumber

    override val visitedFilesNumber: Long
        get() = searchingState.visitedFilesNumber
    override val totalFilesNumber: Long?
        get() = searchingState.totalFilesNumber

    override val visitedFilesByteSize: Long
        get() = searchingState.visitedFilesByteSize

    override val parsedFilesByteSize: Long
        get() = searchingState.parsedFilesByteSize

    override val totalFilesByteSize: Long?
        get() = searchingState.totalFilesByteSize

    override val progress: Double
        get() = if (finished) {
            COMPLETELY_FINISHED_PROGRESS
        } else indexingState.progress * INDEXING_PROGRESS_PART + searchingState.progress * SEARCHING_PROGRESS_PART

    override fun cancel() {
        indexingState.cancel()
        searchingState.cancel()
        cancellationAction()
    }

    override val result: Future<List<TokenMatch>>
        get() = searchingState.result

    override fun getTokenMatchesBuffer(flush: Boolean): List<TokenMatch> = searchingState.getTokenMatchesBuffer(flush)

    override fun getVisitedPathsBuffer(flush: Boolean): List<Path> = searchingState.getVisitedPathsBuffer(flush)

    /**
     * Setting action for cancel.
     * */
    fun addCancellationAction(action: () -> Unit) {
        cancellationAction = action
    }

    companion object {
        private const val COMPLETELY_FINISHED_PROGRESS: Double = 1.0
        private const val INDEXING_PROGRESS_PART: Double = 0.9
        private const val SEARCHING_PROGRESS_PART: Double = COMPLETELY_FINISHED_PROGRESS - INDEXING_PROGRESS_PART

    }
}