package impl.trigram

import api.ProgressableStatus

/**
 * Logic of applying double status from indexing and search status in one for trigramSearchApi
 * */
fun trigramAggregateIndexAndSearchStatus(
    indexStatus: ProgressableStatus,
    searchStatus: ProgressableStatus
): ProgressableStatus =
    when (searchStatus) {
        ProgressableStatus.NOT_STARTED -> when (indexStatus) {
            ProgressableStatus.FINISHED -> ProgressableStatus.IN_PROGRESS
            else -> indexStatus
        }

        ProgressableStatus.IN_PROGRESS -> when (indexStatus) {
            ProgressableStatus.FINISHED -> searchStatus
            else -> ProgressableStatus.FAILED
        }

        ProgressableStatus.CANCELLING -> when (indexStatus) {
            ProgressableStatus.FAILED,
            ProgressableStatus.CANCELLED -> indexStatus

            else -> searchStatus
        }

        ProgressableStatus.CANCELLED -> when (indexStatus) {
            ProgressableStatus.FAILED -> indexStatus
            else -> searchStatus
        }

        ProgressableStatus.FINISHED -> when (indexStatus) {
            ProgressableStatus.CANCELLED,
            ProgressableStatus.FINISHED -> indexStatus

            else -> ProgressableStatus.FAILED
        }

        ProgressableStatus.FAILED -> ProgressableStatus.FAILED
    }
