package api.tools.searchapi.index

import api.IndexingState
import api.SearchApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import utils.diffTime
import utils.format
import utils.prettyMillis
import java.nio.file.Path

/**
 * Util function to calculate index for folder with detailed logging, then after it is done returns
 * Used in tests
 * */
fun SearchApi.syncPerformIndexWithLogging(folderPath: Path) {
    val indexingState = createIndexAtFolder(folderPath)

    runBlocking {
        var lastLogged = indexingState.startTime
        println("started indexing folder $folderPath at ${indexingState.startTime}")
        while (!indexingState.finished) {
            delay(50)
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

/**
 * Prints step of indexing with full details.
 * */
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
        when {
            lastVisitedFileMessage.isNotEmpty()
                    && lastIndexedFileMessage.isNotEmpty() -> ",$lastVisitedFileMessage,$lastIndexedFileMessage"

            lastVisitedFileMessage.isNotEmpty() -> ",$lastVisitedFileMessage"
            lastIndexedFileMessage.isNotEmpty() -> ",$lastIndexedFileMessage"
            else -> ""
        }

    println(
        "indexing folder (visited ${visitedFilesNumber}, indexed ${indexedFilesNumber}, total: $totalMessage) " +
                "${progressPercents.format(2)} %, " +
                "passed time:${prettyMillis(millis)}, " +
                "visited more ${visitedFilesBuffer.size} files, " +
                "indexed more ${indexedFilesBuffer.size} files" + messageEnding
    )
}

/**
 * Progressive scale of steps for indexing
 * */
fun getIndexLogStepMillis(millis: Long) = when (millis) {
    in 0 until 1000 -> 50
    in 1000 until 10_000 -> 1000
    in 10_000 until 60_000 -> 5_000
    in 60_000 until 300_000 -> 15_000
    else -> 60_000
}