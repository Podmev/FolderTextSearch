package impl.indexless

import api.IndexingState
import java.nio.file.Path
import java.util.concurrent.Future

/**
 * State is complete at the start, so no indexing
 * */
class IndexlessIndexingState(override val result: Future<List<Path>>) : IndexingState {
    override val finished: Boolean = true
    override val progress: Double = 1.0
    override fun cancel() { /*nothing to do in this implementation*/}
    override fun getVisitedPathsBuffer(flush: Boolean): List<Path>  = emptyList()
    override fun getIndexedPathsBuffer(flush: Boolean): List<Path>  = emptyList()
    override val visitedFilesNumber: Long = 0
    override val indexedFilesNumber: Long = 0
    override val totalFilesNumber: Long? = null
}