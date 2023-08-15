package impl.indexless

import api.IndexingState
import api.ProgressableStatus
import api.SearchingState
import api.exception.FailedSearchException
import impl.common.IndexingAndSearchingStateAdapter

/**
 * State of indexless search api for searching and indexing.
 * */
class IndexlessIndexingAndSearchingState(
    indexingState: IndexingState,
    searchingState: SearchingState,
) : IndexingAndSearchingStateAdapter(indexingState, searchingState) {

    override val status: ProgressableStatus
        get() = searchingState.status

    override val failReason: Throwable?
        get() = indexingState.failReason ?: searchingState.failReason
        ?: (if (status == ProgressableStatus.FAILED) FailedSearchException("Failed status") else null)

    override val progress: Double
        get() = searchingState.progress

    override fun cancel() {
        /*nothing*/
    }
}