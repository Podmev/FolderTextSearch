package impl.trigram

import api.ProgressableStatus
import api.SearchingState
import api.TokenMatch
import utils.WithLogging
import utils.prettyBytes
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.fileSize
import kotlin.math.min

/**
 * State of trigram search api for searching.
 * */
class TrigramSearchingState(override val result: Future<List<TokenMatch>>) : SearchingState, WithLogging() {
    override val startTime: LocalDateTime = LocalDateTime.now()

    /**
     * Finished time, it should be set on moment of finish, from outside
     */
    @Volatile
    private var finishedTime: LocalDateTime? = null
    override val lastWorkingTime: LocalDateTime
        get() = finishedTime ?: LocalDateTime.now()

    @Volatile
    private var innerStatus: ProgressableStatus = ProgressableStatus.NOT_STARTED
    private val statusMonitor = Any()

    override val status: ProgressableStatus
        get() = innerStatus

    private var innerFailReason: Throwable? = null

    /**
     * Set fail reason
     */
    fun setFailReason(throwable: Throwable) {
        innerFailReason = throwable
    }

    override val failReason: Throwable?
        get() = innerFailReason

    /**
     * Function to change status of search
     */
    fun changeStatus(status: ProgressableStatus) {
        synchronized(statusMonitor) {
            val newStatus = trigramChangeStatus(innerStatus, status)
            if (newStatus != innerStatus) {
                LOG.finest("changing status from $innerStatus to $status")
                innerStatus = newStatus
                if (newStatus.isTerminatingStatus) {
                    finishedTime = LocalDateTime.now()
                }
            }
        }
    }

    //file numbers
    private val visitedFilesNumberRef = AtomicLong(ON_START_COUNTER)
    private val totalFilesNumberRef = AtomicLong(NOT_SET_TOTAL)
    private val totalFilesNumberUpdatedRef = AtomicBoolean(false)

    //file sizes in bytes
    private val visitedFilesByteSizeRef = AtomicLong(ON_START_COUNTER)
    private val parsedFilesByteSizeRef = AtomicLong(ON_START_COUNTER)
    private val totalFilesByteSizeRef = AtomicLong(NOT_SET_TOTAL)
    private val totalFilesByteSizeUpdatedRef = AtomicBoolean(false)

    //token matches
    private val tokenMatchesNumberRef = AtomicLong(ON_START_COUNTER)

    //buffers
    private val visitedPathsBuffer: MutableList<Path> = ArrayList()
    private val tokenMatchesBuffer: MutableList<TokenMatch> = ArrayList()

    //cancel
    @Volatile
    private var cancellationAction: () -> Unit = {}

    override val tokenMatchesNumber: Long
        get() = tokenMatchesNumberRef.get()

    override val visitedFilesNumber: Long
        get() = visitedFilesNumberRef.get()
    override val totalFilesNumber: Long?
        get() = if (totalFilesNumberUpdatedRef.get()) totalFilesNumberRef.get() else null

    override val visitedFilesByteSize: Long
        get() = visitedFilesByteSizeRef.get()

    /**
     * Need to take minimum because of not precise calculation of parsedFilesByteSizeRef
     * */
    override val parsedFilesByteSize: Long
        get() {
            return parsedFilesByteSizeRef.get().let { min(it, totalFilesByteSize ?: return it) }
        }

    override val totalFilesByteSize: Long?
        get() = if (totalFilesByteSizeUpdatedRef.get()) totalFilesByteSizeRef.get() else null

    override val progress: Double
        get() {
            if (finished) {
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
            //We can receive here more than 100%, because of not exact calculation of parsed lines byte sizes
            return if (calculatedProgress >= COMPLETELY_FINISHED_PROGRESS) ALMOST_FINISHED_PROGRESS
            else calculatedProgress
        }

    override fun cancel() {
        cancellationAction()
    }

    override fun getTokenMatchesBuffer(flush: Boolean): List<TokenMatch> {
        synchronized(tokenMatchesBuffer) {
            //making copy of list
            val bufferCopy = ArrayList(tokenMatchesBuffer)
            if (!flush) {
                //Old values we don't erase
                return bufferCopy
            }
            //Need to erase old values
            tokenMatchesBuffer.clear()
            return bufferCopy
        }
    }

    override fun getVisitedPathsBuffer(flush: Boolean): List<Path> {
        synchronized(visitedPathsBuffer) {
            //making copy of list
            val bufferCopy = ArrayList(visitedPathsBuffer)
            if (!flush) {
                //Old values we don't erase
                return bufferCopy
            }
            //Need to erase old values
            visitedPathsBuffer.clear()
            return bufferCopy
        }
    }

    /**
     * Adds newfound token match.
     * */
    fun addTokenMatchToBuffer(tokenMatch: TokenMatch): Long {
        val tokenMatchesNumber = tokenMatchesNumberRef.incrementAndGet()
        LOG.finest("add #$tokenMatchesNumber tokenMatch $tokenMatch")
        synchronized(tokenMatchesBuffer) {
            tokenMatchesBuffer.add(tokenMatch)
        }
        return tokenMatchesNumber
    }

    /**
     * Setting action for cancel.
     * */
    fun addCancellationAction(action: () -> Unit) {
        cancellationAction = action
    }

    /**
     * Adds visited line to state.
     * */
    fun addVisitedPath(path: Path) {
        val fileByteSize = path.fileSize()
        val currentTotalFileByteSize = visitedFilesByteSizeRef.addAndGet(fileByteSize)
        val visitedFileNumber = visitedFilesNumberRef.incrementAndGet()
        LOG.finest(
            "add path $path, visitedFileNumber:$visitedFileNumber, " +
                    "file size: ${prettyBytes(fileByteSize)}, already ${prettyBytes(currentTotalFileByteSize)} files visited"
        )
        synchronized(visitedPathsBuffer) {
            visitedPathsBuffer.add(path)
        }
    }

    /**
     * Adds parsed line to state.
     * */
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
     * Single time is checked by totalFilesNumberUpdatedRef.
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

    /**
     * Approximate string size in bytes as we read them in files
     * */
    private fun String.byteSize(): Int = length * 2

    companion object {
        private const val ALMOST_FINISHED_PROGRESS = 0.9999
        private const val COMPLETELY_FINISHED_PROGRESS = 1.0
        private const val JUST_STARTED_PROGRESS = 0.0

        private const val NOT_SET_TOTAL = -1L
        private const val ON_START_COUNTER = 0L

    }
}