package impl.common

import api.IndexingAndSearchingState
import api.IndexingState
import api.SearchingState
import api.TokenMatch
import utils.WithLogging
import utils.max
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.Future

/**
 * abstractsState of search api for searching and indexing.
 * */
abstract class IndexingAndSearchingStateAdapter(
    override val indexingState: IndexingState,
    override val searchingState: SearchingState,
) : IndexingAndSearchingState, WithLogging() {
    final override val startTime: LocalDateTime
        get() = indexingState.startTime
    final override val lastWorkingTime: LocalDateTime
        get() = max(indexingState.lastWorkingTime, searchingState.lastWorkingTime)
    final override val tokenMatchesNumber: Long
        get() = searchingState.tokenMatchesNumber
    final override val visitedFilesNumber: Long
        get() = searchingState.visitedFilesNumber
    final override val totalFilesNumber: Long?
        get() = searchingState.totalFilesNumber
    final override val visitedFilesByteSize: Long
        get() = searchingState.visitedFilesByteSize
    final override val parsedFilesByteSize: Long
        get() = searchingState.parsedFilesByteSize
    final override val totalFilesByteSize: Long?
        get() = searchingState.totalFilesByteSize
    final override val result: Future<List<TokenMatch>>
        get() = searchingState.result

    final override fun getTokenMatchesBuffer(flush: Boolean): List<TokenMatch> =
        searchingState.getTokenMatchesBuffer(flush)

    final override fun getVisitedPathsBuffer(flush: Boolean): List<Path> = searchingState.getVisitedPathsBuffer(flush)
}