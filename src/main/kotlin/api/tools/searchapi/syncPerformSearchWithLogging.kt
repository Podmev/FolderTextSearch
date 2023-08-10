package api.tools.searchapi

import api.SearchApi
import api.SearchingState
import api.TokenMatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import utils.*
import java.nio.file.Path
import java.time.LocalDateTime

/**
 * Util function to search token in folder with detailed logging, then after it is done returns tokens
 * Used in tests
 * */
fun SearchApi.syncPerformSearchWithLogging(folderPath: Path, token: String, delayMillis: Long = 2L): List<TokenMatch> {
    val startTime = LocalDateTime.now()
    val searchingState = searchString(folderPath, token)
    runBlocking {
        var lastLogged = startTime
        println("started searching folder $folderPath at $startTime")
        while (!searchingState.finished) {
            delay(delayMillis)
            val curTime = LocalDateTime.now()
            val millis = diffTime(startTime, curTime)
            val millisFromLastLogging = diffTime(lastLogged, curTime)
            val logStepMillis = getSearchLogStepMillis(millis)
            if (millisFromLastLogging > logStepMillis) {
                printSearchStepLog(searchingState, millis)
                lastLogged = curTime
            }
        }
        val finishTime = LocalDateTime.now()
        val millis = diffTime(startTime, finishTime)
        printSearchStepLog(searchingState, millis)
    }
    val tokenMatches = searchingState.result.get()!!
    println(
        "searching in folder \"$folderPath\" is finished with ${tokenMatches.size} token matches " +
                "with total time: ${prettyDiffTimeFrom(startTime)}"
    )
    assert(searchingState.finished)
    return tokenMatches
}

/**
 * Prints step of searching with full details.
 * */
fun printSearchStepLog(searchingState: SearchingState, millis: Long) {
    val visitedFilesNumber = searchingState.visitedFilesNumber
    val totalFilesNumber = searchingState.totalFilesNumber

    val visitedFilesByteSize = searchingState.visitedFilesByteSize
    val parsedFilesByteSize = searchingState.parsedFilesByteSize
    val totalFilesByteSize = searchingState.totalFilesByteSize
    val totalMessage: String = if (totalFilesByteSize != null) prettyBytes(totalFilesByteSize)
    else ">=${prettyBytes(visitedFilesByteSize)}"

    val progressPercents = searchingState.progress * 100.0

    val tokenMatchesBuffer = searchingState.getTokenMatchesBuffer(true)
    val lastTokenMatch = tokenMatchesBuffer.lastOrNull()
    val lastTokenMatchesMessage: String = if (lastTokenMatch != null) " last token match: $lastTokenMatch" else ""

    val visitedFilesBuffer = searchingState.getVisitedPathsBuffer(true)
    val lastVisitedPath = visitedFilesBuffer.lastOrNull()
    val lastVisitedFileMessage: String = if (lastVisitedPath != null) " last visited file: $lastVisitedPath" else ""

    val messageEnding =
        when {
            lastTokenMatchesMessage.isNotEmpty()
                    && lastVisitedFileMessage.isNotEmpty() -> ",$lastVisitedFileMessage,$lastTokenMatchesMessage"

            lastTokenMatchesMessage.isNotEmpty() -> ",$lastTokenMatchesMessage"
            lastVisitedFileMessage.isNotEmpty() -> ",$lastVisitedFileMessage"
            else -> ""
        }

    println(
        "searching folder: " +
                "memory(visited ${prettyBytes(visitedFilesByteSize)}, " +
                "parsed ${prettyBytes(parsedFilesByteSize)}, " +
                "total: $totalMessage) " +
                "files(visited: $visitedFilesNumber, total: $totalFilesNumber) " +
                "${progressPercents.format(2)} %, " +
                "passed time:${prettyMillis(millis)}, " +
                "found more ${tokenMatchesBuffer.size} token matches, " +
                "visited more ${visitedFilesBuffer.size} files" + messageEnding
    )
}

/**
 * Progressive scale of steps for searching
 * */
fun getSearchLogStepMillis(millis: Long) = when (millis) {
    in 0 until 10 -> 2
    in 10 until 20 -> 3
    in 20 until 50 -> 5
    in 50 until 100 -> 10
    in 100 until 1000 -> 100
    in 1_000 until 10_000 -> 1_000
    in 10_000 until 30_000 -> 5_000
    else -> 10_000
}