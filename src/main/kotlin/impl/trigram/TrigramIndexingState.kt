package impl.trigram

import api.IndexingState
import utils.WithLogging
import java.nio.file.Path
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

//TODO cancel
//TODO buffer
/*State of trigram search api.
* Has honest:
*  - finished
*  - progress
* */
class TrigramIndexingState(override val result: Future<List<Path>>) : IndexingState, WithLogging() {
    private val visitedFilesNumberRef = AtomicLong(ON_START_VISITED_FILES_NUMBER)
    private val indexedFilesNumberRef = AtomicLong(ON_START_INDEXED_FILES_NUMBER)
    private val totalFilesNumberRef = AtomicLong(NOT_SET_TOTAL_FILES_NUMBER)
    private val totalFilesNumberUpdatedRef = AtomicBoolean(false)

    private val visitedPathsBufferRef = AtomicReference(ArrayList<Path>())
    private val indexedPathsBufferRef = AtomicReference(ArrayList<Path>())
    private val cancelationActionRef = AtomicReference<() -> Unit>(/* no-op */)

    override val visitedFilesNumber: Long
        get() = visitedFilesNumberRef.get()
    override val indexedFilesNumber: Long
        get() = indexedFilesNumberRef.get()
    override val totalFilesNumber: Long?
        get() = if (totalFilesNumberUpdatedRef.get()) totalFilesNumberRef.get() else null

    /*Shows indexing is finished of now, takes value from result future.*/
    override val finished: Boolean
        get() = result.isDone

    /*Progress of indexing. It can have values from 0.0 till 1.0, including both ends.
    * 0.0 means not no progress yet.
    * 1.0 means index finished for 100% (consistent with property 'finished' and `result`).
    * Progress must only not decrease during indexing.
    * If totalFiles = -1, progress is 0.0 (undefined).
    */
    override val progress: Double
        get() {
            if (finished) {
                return COMPLETELY_FINISHED_PROGRESS
            }
            val calculatedProgress = when (val total = totalFilesNumberRef.get()) {
                NOT_SET_TOTAL_FILES_NUMBER -> JUST_STARTED_PROGRESS
                ON_START_INDEXED_FILES_NUMBER -> COMPLETELY_FINISHED_PROGRESS //if there are no files
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
        cancelationActionRef.get()()
    }

    /*Gets current buffer with part of visited files.
    * If flush = true, it returns entire buffer (copied) and set current buffer empty.
    * Else it just returns copy of buffer
    * */
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

    /*Gets current buffer with part of result: indexed files.
    * If flush = true, it returns entire buffer (copied) and set current buffer empty.
    * Else it just returns copy of buffer
    * */
    override fun getIndexedPathsBuffer(flush: Boolean): List<Path> {
        synchronized(indexedPathsBufferRef) {
            if (!flush) {
                //Old values we don't erase
                //making copy of list
                return ArrayList(indexedPathsBufferRef.get())
            }
            //Need to erase old values
            val currentBuffer = indexedPathsBufferRef.getAndSet(ArrayList())
            //TODO better to make copy, but it can work probably without copying
            return ArrayList(currentBuffer)
        }
    }

    /*Adds path to current visited files buffer
    * Slight inconsistance of visitedFilesNumberRef and visitedPathsBufferRef, but it not critical
    * */
    fun addVisitedPathToBuffer(path: Path): Long {
        val visitedFileNumber = visitedFilesNumberRef.incrementAndGet()
        LOG.finest("add path $path, visitedFileNumber:$visitedFileNumber")
        synchronized(visitedPathsBufferRef) {
            visitedPathsBufferRef.get().add(path)
        }
        return visitedFileNumber
    }

    /*Adds path to current result buffer: indexed files
    * Slight inconsistance of indexedFilesNumberRef and indexedPathsBufferRef, but it not critical
    * */
    fun addIndexedPathToBuffer(path: Path): Long{
        val indexedFileNumber = indexedFilesNumberRef.incrementAndGet()
        LOG.finest("add path $path, indexedFileNumber:$indexedFileNumber, total:${totalFilesNumberRef.get()}")
        synchronized(indexedPathsBufferRef) {
            indexedPathsBufferRef.get().add(path)
        }
        return indexedFileNumber
    }

    /*Sets totalFilesNumber maximum single time for live time of TrigramIndexingState.
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

    fun addCancelationAction(action: () -> Unit) {
        cancelationActionRef.set(action)
    }

    companion object {
        private const val ALMOST_FINISHED_PROGRESS = 0.9999
        private const val COMPLETELY_FINISHED_PROGRESS = 1.0
        private const val JUST_STARTED_PROGRESS = 0.0
        private const val NOT_SET_TOTAL_FILES_NUMBER = -1L
        private const val ON_START_INDEXED_FILES_NUMBER = 0L
        private const val ON_START_VISITED_FILES_NUMBER = 0L
    }
}