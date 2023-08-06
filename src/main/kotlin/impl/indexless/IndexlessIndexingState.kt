package impl.indexless

import api.IndexingState
import java.nio.file.Path
import java.util.concurrent.Future

/*State is complete at the start, so no indexing
* */
class IndexlessIndexingState(override val result: Future<List<Path>>) : IndexingState {
    override val finished: Boolean = true
    override val progress: Double = 1.0
    override fun cancel() {}
    override fun getBufferPartResult(flush: Boolean): List<Path> {
        TODO("Not yet implemented")
    }
}