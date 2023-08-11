package api

/**
 * Status of ProgressableState
 * Maybe we need more statuses - Interrupted, Timeout, Error
 */
enum class ProgressableStatus {
    /**
     * Initial status
     */
    NOT_STARTED {
        override val isTerminatingStatus: Boolean
            get() = false
    },

    /**
     * Main working status
     */
    IN_PROGRESS {
        override val isTerminatingStatus: Boolean
            get() = false
    },

    /**
     * Set this status, on call method cancel
     */
    CANCELLING {
        override val isTerminatingStatus: Boolean
            get() = false
    },

    /**
     * On successful cancel.
     */
    CANCELLED {
        override val isTerminatingStatus: Boolean
            get() = true
    },

    /**
     * On successful finish.
     */
    FINISHED {
        override val isTerminatingStatus: Boolean
            get() = true
    },

    /**
     * On error.
     * */
    FAILED {
        override val isTerminatingStatus: Boolean
            get() = true
    };


    /**
     * Tells if status is terminating: canceled or finished or failed
     */
    abstract val isTerminatingStatus: Boolean

}