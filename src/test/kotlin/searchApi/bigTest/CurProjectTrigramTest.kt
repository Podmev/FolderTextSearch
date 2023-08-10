package searchApi.bigTest

import api.SearchApi
import api.tools.syncPerformIndex
import api.tools.syncPerformIndexWithLogging
import api.tools.syncPerformSearchWithLogging
import api.tools.syncSearchToken
import searchApi.common.commonSetup
import impl.trigram.TrigramSearchApi
import utils.prettyDiffTime
import java.nio.file.Path
import java.time.LocalDateTime

/*Searching in current project in source files* */
class CurProjectTrigramTest {
    /*source code of current project* */
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
//        actualTokenMatches.forEach { println(it) }
    }

//    companion object {
//        val popularTokens = listOf(
//            "TODO",
//            "class",
//            "deprecated",
//            "getName",
//            "volatile"
//        )
//    }

}