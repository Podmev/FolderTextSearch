package api.tools.state

import api.ProgressableState
import kotlinx.coroutines.*

/**
 * Util function to cancel state right after progress >= `cancelAtProgress`,
 * and checking every `checkProgressEveryMillis`.
 * Works asynchronously.
 **/
@OptIn(DelicateCoroutinesApi::class)
fun ProgressableState.asyncCancelAtProgress(
    cancelAtProgress: Double,
    checkProgressEveryMillis: Long
): Deferred<Unit> {
    return GlobalScope.async {
        while (!finished) {
            val progress = progress
            if (progress >= cancelAtProgress) {
                cancel()
                break
            }
            delay(checkProgressEveryMillis)
        }
    }
}