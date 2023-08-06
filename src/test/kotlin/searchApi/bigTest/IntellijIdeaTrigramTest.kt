package searchApi.bigTest

import api.SearchApi
import api.tools.syncPerformIndexWithLogging
import api.tools.syncPerformIndexWithLoggingAndCancel
import api.tools.syncSearchToken
import searchApi.common.commonSetup
import impl.trigram.TrigramSearchApi
import utils.prettyDiffTime
import java.nio.file.Path
import java.time.LocalDateTime

/*Searching in intellij idea project*/
class IntellijIdeaTrigramTest {
    /*source code of intellij idea*/
    private val commonPath: Path = commonSetup.intellijIdeaProjectPath.resolve("java")

    private val searchApi: SearchApi = TrigramSearchApi()

    fun justIndex() {
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLogging(folder)
    }

    fun justIndexWithCancel() {
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLoggingAndCancel(folderPathString = folder, cancelAtProgress = 0.00)
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

