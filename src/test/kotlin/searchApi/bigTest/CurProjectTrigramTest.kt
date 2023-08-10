package searchApi.bigTest

import api.SearchApi
import api.tools.searchapi.syncPerformIndex
import api.tools.searchapi.syncPerformIndexWithLogging
import api.tools.searchapi.syncPerformSearchWithLogging
import api.tools.searchapi.syncSearchToken
import impl.trigram.TrigramSearchApi
import searchApi.common.CommonSetup
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
    private val commonPath: Path = CommonSetup.srcFolder

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