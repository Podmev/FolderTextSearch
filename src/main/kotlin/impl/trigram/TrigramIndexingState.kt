package impl.trigram

import api.IndexingState
import api.ProgressableStatus
import utils.WithLogging
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * State of trigram search api for indexing.
 * */
class TrigramIndexingState(override val result: Future<List<Path>>) : IndexingState, WithLogging() {
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
                innerStatus = newStatus
                if (newStatus.isTerminatingStatus) {
                    finishedTime = LocalDateTime.now()
                }
            }
        }
    }

    //file numbers
    private val visitedFilesNumberRef = AtomicLong(ON_START_COUNTER)
    private val indexedFilesNumberRef = AtomicLong(ON_START_COUNTER)
    private val totalFilesNumberRef = AtomicLong(NOT_SET_TOTAL)
    private val totalFilesNumberUpdatedRef = AtomicBoolean(false)

    //buffers
    private val visitedPathsBuffer: MutableList<Path> = ArrayList()
    private val indexedPathsBuffer: MutableList<Path> = ArrayList()

    //cancel
    @Volatile
    private var cancellationAction: () -> Unit = {}

    override val visitedFilesNumber: Long
        get() = visitedFilesNumberRef.get()
    override val indexedFilesNumber: Long
        get() = indexedFilesNumberRef.get()
    override val totalFilesNumber: Long?
        get() = if (totalFilesNumberUpdatedRef.get()) totalFilesNumberRef.get() else null

    /**
     * Progress of indexing. It can have values from 0.0 till 1.0, including both ends.
     * 0.0 means not no progress yet.
     * 1.0 means index finished for 100% (consistent with property 'finished' and `result`).
     * Progress must only not decrease during indexing.
     * If totalFiles = -1, progress is 0.0 (undefined).
     * */
    override val progress: Double
        get() {
            if (finished) {
                return COMPLETELY_FINISHED_PROGRESS
            }
            val calculatedProgress = when (val total = totalFilesNumberRef.get()) {
                NOT_SET_TOTAL -> JUST_STARTED_PROGRESS
                ON_START_COUNTER -> COMPLETELY_FINISHED_PROGRESS //if there are no files
                else -> {
                    val totalDouble = total.toDouble()
                    val curDouble = indexedFilesNumberRef.toDouble()
                    curDouble / totalDouble
                }
            }
            //cannot return 1.0 (100%), if it is not completely finished, so we put artificial value of almost finished
            return if (calculatedProgress == COMPLETELY_FINISHED_PROGRESS) ALMOST_FINISHED_PROGRESS
            else calculatedProgress
        }

    override fun cancel() {
        cancellationAction()
    }

    /**
     * Gets current buffer with part of visited files.
     * If flush = true, it returns entire buffer (copied) and set current buffer empty.
     * Else it just returns copy of buffer
     * */
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
     * Gets current buffer with part of result: indexed files.
     * If flush = true, it returns entire buffer (copied) and set current buffer empty.
     * Else it just returns copy of buffer
     * */
    override fun getIndexedPathsBuffer(flush: Boolean): List<Path> {
        synchronized(indexedPathsBuffer) {
            //making copy of list
            val bufferCopy = ArrayList(indexedPathsBuffer)
            if (!flush) {
                //Old values we don't erase

                return bufferCopy
            }
            //Need to erase old values
            indexedPathsBuffer.clear()
            return bufferCopy
        }
    }

    /**
     * Adds path to current visited files buffer
     * Slight inconsistency of visitedFilesNumberRef and visitedPathsBuffer, but it not critical
     * */
    fun addVisitedPathToBuffer(path: Path): Long {
        val visitedFileNumber = visitedFilesNumberRef.incrementAndGet()
        LOG.finest("add path $path, visitedFileNumber:$visitedFileNumber")
        synchronized(visitedPathsBuffer) {
            visitedPathsBuffer.add(path)
        }
        return visitedFileNumber
    }

    /**
     * Adds path to current result buffer: indexed files
     * Slight inconsistency of indexedFilesNumberRef and indexedPathsBuffer, but it not critical
     * */
    fun addIndexedPathToBuffer(path: Path): Long {
        val indexedFileNumber = indexedFilesNumberRef.incrementAndGet()
        LOG.finest("add path $path, indexedFileNumber:$indexedFileNumber, total:${totalFilesNumberRef.get()}")
        synchronized(indexedPathsBuffer) {
            indexedPathsBuffer.add(path)
        }
        return indexedFileNumber
    }

    /**
     * Sets totalFilesNumber maximum single time for live time of TrigramIndexingState.
     * Single time is checked by setupTotalFilesNumberRef
     * */
    fun setTotalFilesNumber(totalFilesNumber: Long): Boolean {
        LOG.finest("trying to set total:$totalFilesNumber")
        val changed = totalFilesNumberUpdatedRef.compareAndSet(false, true)
        if (changed) {
            totalFilesNumberRef.set(totalFilesNumber)
            LOG.finest("set total:$totalFilesNumber")
        }
        return changed
    }

    /**
     * Setting action for cancel.
     * */
    fun addCancellationAction(action: () -> Unit) {
        cancellationAction = action
    }

    companion object {
        private const val ALMOST_FINISHED_PROGRESS = 0.9999
        private const val COMPLETELY_FINISHED_PROGRESS = 1.0
        private const val JUST_STARTED_PROGRESS = 0.0

        private const val NOT_SET_TOTAL = -1L
        private const val ON_START_COUNTER = 0L
    }
}