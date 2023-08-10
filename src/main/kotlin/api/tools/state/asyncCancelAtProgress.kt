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
    val state = this
    return GlobalScope.async {
        while (!state.finished) {
            val progress = state.progress
            if (progress >= cancelAtProgress) {
                state.cancel()
                break
            }
            delay(checkProgressEveryMillis)
        }
    }
}