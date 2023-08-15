package searchApi.bigTest

import api.SearchApi
import api.tools.searchapi.index.syncPerformIndex
import api.tools.searchapi.index.syncPerformIndexWithLogging
import api.tools.searchapi.search.syncPerformSearchWithLogging
import api.tools.searchapi.search.syncSearchToken
import impl.trigram.TrigramSearchApi
import searchApi.common.commonSetup
import utils.prettyDiffTime
import java.nio.file.Path
import java.time.LocalDateTime

/**
 * Searching in current project in source files
 * */
class CurProjectTrigramTest {
    /**
     * Source code of current project.
     * */
    private val commonPath: Path = commonSetup.srcFolder

    private val searchApi: SearchApi = TrigramSearchApi()

    fun justIndex() {
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLogging(folder)
    }

    fun indexWithSearchOneToken() {
        val token = "class"
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLogging(folder)
        val actualTokenMatches = searchApi.syncSearchToken(folder, token)
        val finishTime = LocalDateTime.now()
        println("total time: ${prettyDiffTime(startTime, finishTime)}")
        println(actualTokenMatches.size)
        actualTokenMatches.forEach { println(it) }
    }

    fun searchOneTokenAfterIndex() {
        val token = "class"
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndex(folder)
        val actualTokenMatches = searchApi.syncPerformSearchWithLogging(folder, token)
        val finishTime = LocalDateTime.now()
        println("total time: ${prettyDiffTime(startTime, finishTime)}")
        println(actualTokenMatches.size)
    }

}