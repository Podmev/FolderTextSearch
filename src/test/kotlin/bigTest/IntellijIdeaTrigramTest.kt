package bigTest

import api.SearchApi
import api.tools.syncPerformIndexWithLogging
import api.tools.syncSearchToken
import common.commonSetup
import trigram.TrigramSearchApi
import utils.prettyDiffTime
import java.nio.file.Path
import java.time.LocalDateTime

/*Searching in intellij idea project*/
class IntellijIdeaTrigramTest {
    /*source code of intellij idea*/
    private val commonPath: Path = commonSetup.intellijIdeaProjectPath.resolve("java")

    private val searchApi: SearchApi = TrigramSearchApi()

    /*doesn't finish in 12 minutes. how long in fact I don't know
    * */
    fun justIndex() {
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLogging(folder)
    }

    fun indexWithSearchOneToken() {
        val token = "class"
        val folder = commonPath
        searchApi.syncPerformIndexWithLogging(folder)
        val startTime = LocalDateTime.now()
        val actualTokenMatches = searchApi.syncSearchToken(folder, token)
        val finishTime = LocalDateTime.now()
        println("searching time for looking for token \"${token}\": ${prettyDiffTime(startTime, finishTime)}, #${actualTokenMatches.size}")
    }

    fun indexWithSearchManyTokens() {
        val folder = commonPath
        searchApi.syncPerformIndexWithLogging(folder)
        for(token in popularTokens) {
            val startTime = LocalDateTime.now()
            val actualTokenMatches = searchApi.syncSearchToken(folder, token)
            val finishTime = LocalDateTime.now()
            println("searching time for looking for token \"${token}\": ${prettyDiffTime(startTime, finishTime)}, #${actualTokenMatches.size}")
            println(actualTokenMatches.size)
        }
    }

//    fun jarTest(){
//        val jarPath = commonPath
//            .resolve("execution")
//            .resolve("impl")
//            .resolve("jshell-frontend.jar")
//
//        println(TrigramSearchApi().isPossibleToIndexFile(jarPath))
//    }

    companion object {
        val popularTokens = listOf(
            "TODO",
            "class",
            "deprecated",
            "getName",
            "volatile"
        )
    }

}

