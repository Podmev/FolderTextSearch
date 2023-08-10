package impl.indexless

import api.SearchingState
import api.TokenMatch
import api.exception.SearchException
import java.nio.file.Path
import java.util.concurrent.Future


/**
 * Simplified SearchingState, with not everything implemented
 * */
class IndexlessSearchingState(override val result: Future<List<TokenMatch>>) : SearchingState {
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

    override fun getTokenMatchesBuffer(flush: Boolean): List<TokenMatch> {
        throw SearchException("Not supported getTokenMatchesBuffer for searching in indexless api")
    }

    override fun getVisitedPathsBuffer(flush: Boolean): List<Path> {
        throw SearchException("Not supported getVisitedPathsBuffer for searching in indexless api")
    }

    override val visitedFilesNumber: Long = 0L
    override val totalFilesNumber: Long? = null

    override val visitedFilesByteSize: Long = 0L
    override val parsedFilesByteSize: Long = 0L
    override val totalFilesByteSize: Long? = null
}