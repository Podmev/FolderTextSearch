package utils.coroutines

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

//TODO think where to use
/*Forced point for cancel opportunity*/
suspend fun makeCancelablePoint() =
    suspendCancellableCoroutine { continuation ->
        continuation.resume(Unit)
    }
