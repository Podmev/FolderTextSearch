package impl.indexless

import api.ProgressableStatus
import api.SearchingState
import api.TokenMatch
import api.exception.SearchException
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.Future


/**
 * Simplified SearchingState, with not everything implemented
 * */
class IndexlessSearchingState(override val result: Future<List<TokenMatch>>) : SearchingState {
    override val startTime: LocalDateTime = LocalDateTime.now()

    /**
     * Finished time, it should be set on moment of finish, from outside
     */
    private var finishedTime: LocalDateTime? = null
    override val lastWorkingTime: LocalDateTime
        get() = finishedTime ?: LocalDateTime.now()

    private var innerStatus: ProgressableStatus = ProgressableStatus.NOT_STARTED

    override val status: ProgressableStatus
        get() = innerStatus

    override val failReason: Throwable? = null

    /**
     * Function to change status of search
     */
    fun changeStatus(status: ProgressableStatus) {
        when (status) {
            ProgressableStatus.NOT_STARTED -> {/*do nothing*/
            }

            ProgressableStatus.IN_PROGRESS -> {/*do nothing*/
            }

            ProgressableStatus.CANCELLING -> throw SearchException("Not supported status Cancelling")
            ProgressableStatus.CANCELLED -> throw SearchException("Not supported status Canceled")
            ProgressableStatus.FINISHED -> finishedTime = LocalDateTime.now()
            ProgressableStatus.FAILED -> throw SearchException("Not supported status Failed")
        }
        innerStatus = status
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

    override val tokenMatchesNumber: Long = 0L
    override val visitedFilesNumber: Long = 0L
    override val totalFilesNumber: Long? = null

    override val visitedFilesByteSize: Long = 0L
    override val parsedFilesByteSize: Long = 0L
    override val totalFilesByteSize: Long? = null
}