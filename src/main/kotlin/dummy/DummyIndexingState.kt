package dummy

import api.IndexingState
import java.util.concurrent.Future

/*State is complete at the start, so no indexing
* */
class DummyIndexingState(override val result: Future<List<String>>) : IndexingState {
    override val finished: Boolean = true
    override val progress: Double = 1.0
    override fun cancel() {}
    override fun getBufferPartResult(flush: Boolean): List<String> {
        TODO("Not yet implemented")
    }
}