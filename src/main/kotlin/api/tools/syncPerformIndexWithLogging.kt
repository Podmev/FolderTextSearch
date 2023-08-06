package api.tools

import api.IndexingState
import api.SearchApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import utils.diffTime
import utils.format
import utils.prettyDiffTimeFrom
import utils.prettyMillis
import java.nio.file.Path
import java.time.LocalDateTime

//TODO refactor to class with using logging
/*Util function to calculate index for folder with detailed logging, then after it is done returns
* Used in tests
* */
fun SearchApi.syncPerformIndexWithLogging(folderPath: Path) {
    val startTime = LocalDateTime.now()
    val indexingState = createIndexAtFolder(folderPath)
    runBlocking {
        async {
            var lastLogged = startTime
            println("started indexing folder $folderPath at $startTime")
            while (!indexingState.finished) {
                delay(50)
                val curTime = LocalDateTime.now()
                val millis = diffTime(startTime, curTime)
                val millisFromLastLogging = diffTime(lastLogged, curTime)
                val logStepMillis = getLogStepMillis(millis)
                if (millisFromLastLogging > logStepMillis) {
                    printStepLog(indexingState, millis)
                    lastLogged = curTime
                }
            }
            val finishTime = LocalDateTime.now()
            val millis = diffTime(startTime, finishTime)
            printStepLog(indexingState, millis)
        }
    }
    val paths = indexingState.result.get()!!
    println("indexing folder \"$folderPath\" is finished with ${paths.size} paths " +
            "with total time: ${prettyDiffTimeFrom(startTime)}")
    assert(indexingState.finished)
}

fun printStepLog(indexingState: IndexingState, millis: Long) {
    val progressPercents = indexingState.progress * 100.0
    val indexedFilesBuffer = indexingState.getBufferPartResult(true)
    val lastPath = indexedFilesBuffer.lastOrNull()
    val lastFileMessage: String = if (lastPath != null) ", last file: $lastPath" else ""
    println(
        "indexing folder ${progressPercents.format(2)} %, " +
                "passed time:${prettyMillis(millis)}, " +
                "indexed more ${indexedFilesBuffer.size} files$lastFileMessage"
    )
}

fun getLogStepMillis(millis: Long) = when (millis) {
    in 0 until 1000 -> 50
    in 1000 until 10_000 -> 1000
    in 10_000 until 60_000 -> 5_000
    in 60_000 until 300_000 -> 15_000
    else -> 60_000
}