package api.tools.searchapi.indexAndSearch

import api.IndexingAndSearchingState
import api.SearchApi
import api.TokenMatch
import api.tools.searchapi.search.getSearchLogStepMillis
import api.tools.searchapi.search.printSearchStepLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import utils.diffTime
import utils.prettyMillis
import java.nio.file.Path

/**
 * Util function to index in folder and then search token in folder with detailed logging,
 * then after it is done returns tokens.
 * Used in tests
 * */
fun SearchApi.syncPerformIndexAndSearchWithLogging(
    folderPath: Path,
    token: String,
    delayMillis: Long = 2L
): List<TokenMatch> {
    val indexingAndSearchingState: IndexingAndSearchingState = indexAndSearchString(folderPath, token)
    runBlocking {
        var lastLogged = indexingAndSearchingState.startTime
        println("started indexing and searching folder $folderPath at ${indexingAndSearchingState.startTime}")
        while (!indexingAndSearchingState.finished) {
            delay(delayMillis)
            val curTime = indexingAndSearchingState.lastWorkingTime
            val millisFromLastLogging = diffTime(lastLogged, curTime)
            val logStepMillis = getSearchLogStepMillis(indexingAndSearchingState.totalTime)
            if (millisFromLastLogging > logStepMillis) {
                printSearchStepLog(indexingAndSearchingState, indexingAndSearchingState.totalTime)
                lastLogged = curTime
            }
        }
        printSearchStepLog(indexingAndSearchingState, indexingAndSearchingState.totalTime)
    }
    val tokenMatches = indexingAndSearchingState.result.get()!!
    println(
        "indexing and searching in folder \"$folderPath\" is finished with ${tokenMatches.size} token matches " +
                "with total time: ${prettyMillis(indexingAndSearchingState.totalTime)}"
    )
    assert(indexingAndSearchingState.finished)
    return tokenMatches
}
