package api.tools.searchapi.index

import api.SearchApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import utils.diffTime
import utils.prettyMillis
import java.nio.file.Path

/**
 * Util function to calculate index for folder with detailed logging, then after it is done returns
 * Used in tests
 * */
fun SearchApi.syncPerformIndexWithLoggingAndCancel(folderPath: Path, cancelAtProgress: Double) {
    val indexingState = createIndexAtFolder(folderPath)
    runBlocking {
        var lastLogged = indexingState.startTime
        println("started indexing folder $folderPath at ${indexingState.startTime}")
        var setCancelled = false
        while (!indexingState.finished) {
            delay(50)
            val progress = indexingState.progress
            if (!setCancelled && progress >= cancelAtProgress) {
                indexingState.cancel()
                setCancelled = true
                println("cancel at progress $progress (>=${cancelAtProgress})")
            }
            val curTime = indexingState.lastWorkingTime
            val millisFromLastLogging = diffTime(lastLogged, curTime)
            val logStepMillis = getIndexLogStepMillis(indexingState.totalTime)
            if (millisFromLastLogging > logStepMillis) {
                printIndexingStepLog(indexingState, indexingState.totalTime)
                lastLogged = curTime
            }
        }
        printIndexingStepLog(indexingState, indexingState.totalTime)
    }
    val paths = indexingState.result.get()!!
    println(
        "indexing folder \"$folderPath\" is finished with ${paths.size} paths " +
                "with total time: ${prettyMillis(indexingState.totalTime)}"
    )
    assert(indexingState.finished)
}

