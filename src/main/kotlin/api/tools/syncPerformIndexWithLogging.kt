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
    println("indexing folder \"$folderPath\" is finished with ${paths.size} paths " +
            "with total time: ${prettyDiffTimeFrom(startTime)}")
    assert(indexingState.finished)
}

fun printIndexingStepLog(indexingState: IndexingState, millis: Long) {
    val visitedFilesNumber = indexingState.visitedFilesNumber
    val indexedFilesNumber = indexingState.indexedFilesNumber
    val totalFilesNumber = indexingState.totalFilesNumber
    val totalMessage: String = if (totalFilesNumber != null) "$totalFilesNumber" else ">=${visitedFilesNumber}"

    val progressPercents = indexingState.progress * 100.0

    val visitedFilesBuffer = indexingState.getVisitedPathsBuffer(true)
    val lastVisitedPath = visitedFilesBuffer.lastOrNull()
    val lastVisitedFileMessage: String = if (lastVisitedPath != null) " last visited file: $lastVisitedPath" else ""

    val indexedFilesBuffer = indexingState.getIndexedPathsBuffer(true)
    val lastIndexedPath = indexedFilesBuffer.lastOrNull()
    val lastIndexedFileMessage: String = if (lastIndexedPath != null) " last indexed file: $lastIndexedPath" else ""

    val messageEnding =
        if(lastVisitedFileMessage.isNotEmpty() && lastIndexedFileMessage.isNotEmpty()) ",$lastVisitedFileMessage,$lastIndexedFileMessage"
        else if(lastVisitedFileMessage.isNotEmpty()) ",$lastVisitedFileMessage"
        else if(lastIndexedFileMessage.isNotEmpty()) ",$lastIndexedFileMessage"
        else ""

    println(
        "indexing folder (visited ${visitedFilesNumber}, indexed ${indexedFilesNumber}, total: $totalMessage) " +
                "${progressPercents.format(2)} %, " +
                "passed time:${prettyMillis(millis)}, " +
                "visited more ${visitedFilesBuffer.size} files, " +
                "indexed more ${indexedFilesBuffer.size} files" + messageEnding
    )
}

fun getIndexLogStepMillis(millis: Long) = when (millis) {
    in 0 until 1000 -> 50
    in 1000 until 10_000 -> 1000
    in 10_000 until 60_000 -> 5_000
    in 60_000 until 300_000 -> 15_000
    else -> 60_000
}