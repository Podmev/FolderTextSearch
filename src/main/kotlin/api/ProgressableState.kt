package api

import utils.diffTime
import java.time.LocalDateTime

/**
 * State, that shows progress of some continuous task.
 * Can be canceled.
 * Can be finished.
 * Can show progress.
 */
interface ProgressableState {
    /**
     * status of task
     */
    val status: ProgressableStatus

    /**
     * Reason why task failed
     * */
    val failReason: Throwable?

    /**
     * Shows if task is finished.
     * */
    val finished: Boolean
        get() = status == ProgressableStatus.FINISHED

    /**
     * Shows if task is canceled.
     * */
    val canceled: Boolean
        get() = status == ProgressableStatus.CANCELLED

    /**
     * Can be from 0 till 1 inclusive borders, where 0 means not started, and 1  - finished.
     * If task cancels, progress stays with the same value
     * */
    val progress: Double

    /**
     * Method to cancel the process of task.
     * It can be useful, if the task takes long time.
     * */
    fun cancel()

    /**
     * Moment of starting task
     * */
    val startTime: LocalDateTime

    /**
     * Moment of finish or cancel task, otherwise it is now
     * */
    val lastWorkingTime: LocalDateTime

    /**
     * How long task is going already, if it is in progress, it counts till now
     * */
    val totalTime: Long
        get() = diffTime(startTime, lastWorkingTime)
}