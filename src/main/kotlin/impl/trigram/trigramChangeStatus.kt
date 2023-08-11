package impl.trigram

import api.ProgressableStatus

/**
 * Logic of changing statuses for trigramSearchApi
 * */
fun trigramChangeStatus(previousStatus: ProgressableStatus, newStatus: ProgressableStatus): ProgressableStatus {
    when (newStatus) {
        ProgressableStatus.NOT_STARTED -> {
            //do nothing
        }

        ProgressableStatus.IN_PROGRESS -> {
            when (previousStatus) {
                ProgressableStatus.NOT_STARTED -> {
                    return newStatus
                }

                else -> {/* do nothing*/
                }
            }
        }

        ProgressableStatus.CANCELLING -> {
            when (previousStatus) {
                ProgressableStatus.NOT_STARTED,
                ProgressableStatus.IN_PROGRESS -> {
                    return newStatus
                }

                else -> {/* do nothing*/
                }
            }
        }

        ProgressableStatus.CANCELLED, ProgressableStatus.FAILED -> {
            when (previousStatus) {
                ProgressableStatus.NOT_STARTED,
                ProgressableStatus.IN_PROGRESS,
                ProgressableStatus.CANCELLING -> {
                    return newStatus
                }

                else -> {/*do nothing*/
                }
            }
        }

        ProgressableStatus.FINISHED -> {
            when (previousStatus) {
                ProgressableStatus.NOT_STARTED,
                ProgressableStatus.IN_PROGRESS -> {
                    return newStatus
                }

                else -> {/*do nothing*/
                }
            }
        }
    }
    return previousStatus
}