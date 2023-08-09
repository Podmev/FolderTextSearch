package impl.trigram

import api.SearchingState
import api.TokenMatch
import utils.WithLogging
import utils.prettyBytes
import java.nio.file.Path
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.fileSize

/**
 * State of trigram search api for searching.
 */
class TrigramSearchingState(override val result: Future<List<TokenMatch>>) : SearchingState, WithLogging() {
    private val visitedFilesNumberRef = AtomicLong(ON_START_COUNTER)
    private val totalFilesNumberRef = AtomicLong(NOT_SET_TOTAL)
    private val totalFilesNumberUpdatedRef = AtomicBoolean(false)

    private val visitedFilesByteSizeRef = AtomicLong(ON_START_COUNTER)
    private val parsedFilesByteSizeRef = AtomicLong(ON_START_COUNTER)
    private val totalFilesByteSizeRef = AtomicLong(NOT_SET_TOTAL)
    private val totalFilesByteSizeUpdatedRef = AtomicBoolean(false)

    private val tokenMatchesNumberRef = AtomicLong(ON_START_COUNTER)

    private val visitedPathsBufferRef = AtomicReference(ArrayList<Path>())
    private val tokenMatchesBufferRef = AtomicReference(ArrayList<TokenMatch>())
    private val cancellationActionRef = AtomicReference<() -> Unit>(/* no-op */)

    override val visitedFilesNumber: Long
        get() = visitedFilesNumberRef.get()
    override val totalFilesNumber: Long?
        get() = if (totalFilesNumberUpdatedRef.get()) totalFilesNumberRef.get() else null

    override val visitedFilesByteSize: Long
        get() = visitedFilesByteSizeRef.get()
    override val parsedFilesByteSize: Long
        get() = parsedFilesByteSizeRef.get()
    override val totalFilesByteSize: Long?
        get() = if (totalFilesByteSizeUpdatedRef.get()) totalFilesByteSizeRef.get() else null

    override val finished: Boolean
        get() = result.isDone

    override val progress: Double
        get() {
            if(finished){
                return COMPLETELY_FINISHED_PROGRESS
            }
            val calculatedProgress = when (val total = totalFilesByteSizeRef.get()) {
                NOT_SET_TOTAL -> JUST_STARTED_PROGRESS
                ON_START_COUNTER -> COMPLETELY_FINISHED_PROGRESS //if there are no files
                else -> {
                    val totalDouble = total.toDouble()
                    val curDouble = parsedFilesByteSizeRef.toDouble()
                    /*Since parsedFilesByteSizeRef is not accurate, it will be a bit less.
                     *But it doesn't look like not a problem
                    * */
                    curDouble / totalDouble
                }
            }
            //cannot return 1.0 (100%), if it is not completely finished, so we put artificial value of almost finished
            return if (calculatedProgress == COMPLETELY_FINISHED_PROGRESS) ALMOST_FINISHED_PROGRESS
            else calculatedProgress
        }

    override fun cancel() {
        cancellationActionRef.get()()
    }

    override fun getTokenMatchesBuffer(flush: Boolean): List<TokenMatch> {
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

    override fun getVisitedPathsBuffer(flush: Boolean): List<Path> {
        synchronized(visitedPathsBufferRef) {
            if (!flush) {
                //Old values we don't erase
                //making copy of list
                return ArrayList(visitedPathsBufferRef.get())
            }
            //Need to erase old values
            val currentBuffer = visitedPathsBufferRef.getAndSet(ArrayList())
            //TODO better to make copy, but it can work probably without copying
            return ArrayList(currentBuffer)
        }
    }

    /**
     * Adds newfound token match
     */
    fun addTokenMatchToBuffer(tokenMatch: TokenMatch): Long {
        val tokenMatchesNumber = tokenMatchesNumberRef.incrementAndGet()
        LOG.finest("add #$tokenMatchesNumber tokenMatch $tokenMatch")
        synchronized(tokenMatchesBufferRef) {
            tokenMatchesBufferRef.get().add(tokenMatch)
        }
        return tokenMatchesNumber
    }

    /**
     * Adds cancellation action.
     */
    fun addCancellationAction(action: () -> Unit) {
        cancellationActionRef.set(action)
    }

    /**
     * Adds visited line to state
     */
    fun addVisitedPath(path: Path) {
        val fileByteSize = path.fileSize()
        val currentTotalFileByteSize = visitedFilesByteSizeRef.addAndGet(fileByteSize)
        val visitedFileNumber = visitedFilesNumberRef.incrementAndGet()
        LOG.finest("add path $path, visitedFileNumber:$visitedFileNumber, " +
                "file size: ${prettyBytes(fileByteSize)}, already ${prettyBytes(currentTotalFileByteSize)} files visited")
        synchronized(visitedPathsBufferRef) {
            visitedPathsBufferRef.get().add(path)
        }
        return
    }

    /**
     * Adds parsed line to state
     */
    fun addParsedLine(line: String) {
        parsedFilesByteSizeRef.addAndGet(line.byteSize().toLong())
    }

    /**
     * Sets totalFilesByteSize maximum single time for live time of TrigramSearchingState.
     * Single time is checked by totalFilesByteSizeUpdatedRef
     * */
    fun setTotalFilesByteSize(): Boolean {
        val totalFilesByteSize = visitedFilesByteSize
        LOG.finest("trying to set total files byte size:$totalFilesByteSize")
        val changed = totalFilesByteSizeUpdatedRef.compareAndSet(false, true)
        if (changed) {
            totalFilesByteSizeRef.set(totalFilesByteSize)
            LOG.finest("set total files byte size:$totalFilesByteSize")
        }
        return changed
    }

    /**
     * Sets totalFilesNumber maximum single time for live time of TrigramSearchingState.
     * Single time is checked by totalFilesNumberUpdatedRef
     * */
    fun setTotalFilesNumber(): Boolean {
        val totalFilesNumber = visitedFilesNumber
        LOG.finest("trying to set total files number:$totalFilesNumber")
        val changed = totalFilesNumberUpdatedRef.compareAndSet(false, true)
        if (changed) {
            totalFilesNumberRef.set(totalFilesNumber)
            LOG.finest("set total files number:$totalFilesNumber")
        }
        return changed
    }

    /*approximate string size in bytes as we read them in files*/
    private fun String.byteSize(): Int = length * 2

    companion object {
        private const val ALMOST_FINISHED_PROGRESS = 0.9999
        private const val COMPLETELY_FINISHED_PROGRESS = 1.0
        private const val JUST_STARTED_PROGRESS = 0.0

        private const val NOT_SET_TOTAL = -1L
        private const val ON_START_COUNTER = 0L

    }
}