package bigTest

import api.SearchApi
import api.tools.syncPerformIndexWithLogging
import api.tools.syncSearchToken
import common.commonSetup
import trigram.TrigramSearchApi
import utils.prettyDiffTime
import java.nio.file.Path
import java.time.LocalDateTime

/*Searching in current project in source files*/
class CurProjectTrigramTest {
    /*source code of intellij idea*/
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