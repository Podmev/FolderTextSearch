package dummy

import api.IndexingState

/*State is complete at the start, so no indexing
* */
class DummyIndexingState() : IndexingState {
    override fun isFinished(): Boolean = true
    override fun cancel() {}
    override fun getProgress(): Double = 1.0
}