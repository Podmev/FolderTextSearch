package api.tools

import api.SearchApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import utils.diffTime
import utils.prettyDiffTimeFrom
import java.nio.file.Path
import java.time.LocalDateTime

//TODO refactor to class with using logging
/*Util function to calculate index for folder with detailed logging, then after it is done returns
* Used in tests
* */
fun SearchApi.syncPerformIndexWithLoggingAndCancel(folderPathString: Path, cancelAtProgress: Double) {
    val startTime = LocalDateTime.now()
    val indexingState = createIndexAtFolder(folderPathString)
    runBlocking {
        async {
            var lastLogged = startTime
            println("started indexing folder $folderPathString at $startTime")
            var setCancelled = false
            while (!indexingState.finished) {
                delay(50)
                val progress = indexingState.progress
                if (!setCancelled && progress >= cancelAtProgress) {
                    indexingState.cancel()
                    setCancelled = true
                    println("cancel at progress $progress (>=${cancelAtProgress})")
                }
                val curTime = LocalDateTime.now()
                val millis = diffTime(startTime, curTime)
                val millisFromLastLogging = diffTime(lastLogged, curTime)
                val logStepMillis = getIndexLogStepMillis(millis)
                if (millisFromLastLogging > logStepMillis) {
                    printIndexingStepLog(indexingState, millis)
                    lastLogged = curTime
                }
            }
            val finishTime = LocalDateTime.now()
            val millis = diffTime(startTime, finishTime)
            printIndexingStepLog(indexingState, millis)
        }
    }
    val paths = indexingState.result.get()!!
    println(
        "indexing folder \"$folderPathString\" is finished with ${paths.size} paths " +
                "with total time: ${prettyDiffTimeFrom(startTime)}"
    )
    assert(indexingState.finished)
}

