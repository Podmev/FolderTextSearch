package dummy

import api.SearchException
import api.SearchingState

class DummySearchingState : SearchingState {
    private var currentResult = DummySearchResult(emptyList(), 0)
    private var _finished = false

    override val finished: Boolean
        get() {
            return _finished
        }

    override val progress: Double
        get() {
            return if (_finished) 1.0 else 0.0
        }

    override val result: DummySearchResult
        get() {
            return currentResult
        }

    override fun cancel() {
        throw SearchException("Not supported cancel for searching in dummy api")
    }

    fun setCurrentSearchResult(searchResult: DummySearchResult) {
        currentResult = searchResult
        _finished = true
    }
}