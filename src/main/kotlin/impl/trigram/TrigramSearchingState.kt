package impl.trigram

import api.exception.SearchException
import api.SearchingState
import api.TokenMatch
import java.util.concurrent.Future

//TODO implement correct
class TrigramSearchingState(override val result: Future<List<TokenMatch>>) : SearchingState {
    override val finished: Boolean
        get() {
            return result.isDone
        }

    override val progress: Double
        get() {
            return if (finished) 1.0 else 0.0
        }

    override fun cancel() {
        throw SearchException("Not supported cancel for searching in indexless api")
    }

    override fun getBufferPartResult(flush: Boolean): List<TokenMatch> {
        TODO("Not yet implemented")
    }
}