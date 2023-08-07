package impl.trigram

import api.SearchingState
import api.TokenMatch
import utils.WithLogging
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

//TODO implement correctly progress
class TrigramSearchingState(override val result: Future<List<TokenMatch>>) : SearchingState, WithLogging() {
    private val tokenMatchesNumberRef = AtomicLong(ON_START_TOKEN_MATCH_NUMBER)

    private val tokenMatchesBufferRef = AtomicReference(ArrayList<TokenMatch>())
    private val cancelationActionRef = AtomicReference<() -> Unit>(/* no-op */)
    override val finished: Boolean
        get() {
            return result.isDone
        }

    override val progress: Double
        get() {
            return if (finished) 1.0 else 0.0 //TODO make better counter
        }

    override fun cancel() {
        cancelationActionRef.get()()
    }

    override fun getBufferPartResult(flush: Boolean): List<TokenMatch> {
        synchronized(tokenMatchesBufferRef) {
            if (!flush) {
                //Old values we don't erase
                //making copy of list
                return ArrayList(tokenMatchesBufferRef.get())
            }
            //Need to erase old values
            val currentBuffer = tokenMatchesBufferRef.getAndSet(ArrayList())
            //TODO better to make copy, but it can work probably without copying
            return ArrayList(currentBuffer)
        }
    }

    fun addTokenMatchToBuffer(tokenMatch: TokenMatch): Long {
        val tokenMatchesNumber = tokenMatchesNumberRef.incrementAndGet()
        LOG.finest("add #$tokenMatchesNumber tokenMatch $tokenMatch")
        synchronized(tokenMatchesBufferRef) {
            tokenMatchesBufferRef.get().add(tokenMatch)
        }
        return tokenMatchesNumber
    }

    fun addCancelationAction(action: () -> Unit) {
        cancelationActionRef.set(action)
    }

    companion object {
        private const val ON_START_TOKEN_MATCH_NUMBER = 0L
    }
}