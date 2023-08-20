package api

/**
 * Status of ProgressableState
 * Maybe we need more statuses - Interrupted, Timeout, Error
 * @param isTerminatingStatus - tells if status is terminating: canceled or finished or failed
 */
enum class ProgressableStatus(
    val isTerminatingStatus: Boolean
) {
    /**
     * Initial status
     */
    NOT_STARTED(false),

    /**
     * Main working status
     */
    IN_PROGRESS(false),

    /**
     * Set this status, on call method cancel
     */
    CANCELLING(false),

    /**
     * On successful cancel.
     */
    CANCELLED(true),

    /**
     * On successful finish.
     */
    FINISHED(true),

    /**
     * On error.
     * */
    FAILED(true);
}