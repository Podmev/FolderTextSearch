package trigram

import api.IndexingState
import java.nio.file.Path
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

//TODO cancel
//TODO buffer
/*State of trigram search api.
* Has honest:
*  - finished
*  - progress
* */
class TrigramIndexingState(override val result: Future<List<Path>>) : IndexingState {
    private val indexedFilesNumberRef = AtomicLong(ON_START_INDEXED_FILES_NUMBER)
    private val totalFilesNumberRef = AtomicLong(NOT_SET_TOTAL_FILES_NUMBER)
    private val totalFilesNumberUpdatedRef = AtomicBoolean(false)

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
                    val curDouble = totalFilesNumberRef.toDouble()
                    curDouble / totalDouble
                }
            }
            //cannot return 1.0 (100%), if it is not completely finished, so we put artificial value of almost finished
            return if (calculatedProgress == COMPLETELY_FINISHED_PROGRESS) ALMOST_FINISHED_PROGRESS
            else calculatedProgress
        }

    override fun cancel() {}
    override fun getBufferPartResult(flush: Boolean): List<Path> {
        //TODO("Not yet implemented")
        return emptyList()
    }

    fun addPathToBuffer(path: Path) {
        indexedFilesNumberRef.incrementAndGet()
        //TODO saving to local buffer
    }

    /*Sets totalFilesNumber maximum single time for live time of TrigramIndexingState.
    * Single time is checked by setupTotalFilesNumberRef
    * */
    fun setTotalFilesNumber(totalFilesNumber: Long): Boolean {
        val changed = totalFilesNumberUpdatedRef.compareAndSet(false, true)
        if (changed) {
            totalFilesNumberRef.set(totalFilesNumber)
        }
        return changed
    }

    companion object {
        private const val ALMOST_FINISHED_PROGRESS = 0.9999
        private const val COMPLETELY_FINISHED_PROGRESS = 1.0
        private const val JUST_STARTED_PROGRESS = 1.0
        private const val NOT_SET_TOTAL_FILES_NUMBER = -1L
        private const val ON_START_INDEXED_FILES_NUMBER = 0L
    }
}