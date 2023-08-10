package api

/**
 * State, that shows progress of some continuous task.
 * Can be canceled.
 * Can be finished.
 * Can show progress.
 */
interface ProgressableState {
    /**
     * Shows task is finished.
     * */
    val finished: Boolean

    /**
     * Can be from 0 till 1 inclusive borders, where 0 means not started, and 1  - finished.
     * */
    val progress: Double

    /**
     * Method to cancel the process of task.
     * It can be useful, if the task takes long time.
     * */
    fun cancel()
}