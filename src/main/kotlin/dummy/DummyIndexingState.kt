package dummy

import api.IndexingState

/*State is complete at the start, so no indexing
* */
class DummyIndexingState : IndexingState {
    override val finished: Boolean = true
    override val progress: Double = 1.0
    override fun cancel() {}
}