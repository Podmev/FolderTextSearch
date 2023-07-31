package dummy

import api.SearchException
import api.SearchingState

class DummySearchingState : SearchingState {
    private var currentResult = DummySearchResult(emptyList(), 0)
    private var finished = false

    override fun isFinished(): Boolean {
        return finished
    }

    override fun cancel() {
        throw SearchException("Not supported cancel for searching in dummy api")
    }

    override fun getProgress(): Double = if (finished) 1.0 else 0.0

    override fun getResult(): DummySearchResult = currentResult

    fun setCurrentSearchResult(searchResult: DummySearchResult) {
        currentResult = searchResult
        finished = true
    }
}