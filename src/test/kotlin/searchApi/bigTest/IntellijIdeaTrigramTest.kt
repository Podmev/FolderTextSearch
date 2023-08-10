package searchApi.bigTest

import api.SearchApi
import api.tools.searchapi.syncPerformIndexWithLogging
import api.tools.searchapi.syncPerformIndexWithLoggingAndCancel
import api.tools.searchapi.syncPerformSearchWithLogging
import api.tools.searchapi.syncSearchToken
import impl.trigram.TrigramSearchApi
import searchApi.common.commonSetup
import utils.prettyDiffTime
import java.nio.file.Path
import java.time.LocalDateTime

/**
 * Searching in Intellij Idea project
 * */
class IntellijIdeaTrigramTest {
    /**
     * Source code of Intellij Idea
     * */
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
        println(
            "searching time for looking for token \"${token}\": ${
                prettyDiffTime(
                    startTime,
                    finishTime
                )
            }, #${actualTokenMatches.size}"
        )
    }

    fun indexWithSearchManyTokens() {
        val folder = commonPath
        searchApi.syncPerformIndexWithLogging(folder)
        for (token in popularTokens) {
            val startTime = LocalDateTime.now()
            val actualTokenMatches = searchApi.syncSearchToken(folder, token)
            val finishTime = LocalDateTime.now()
            println(
                "searching time for looking for token \"${token}\": ${
                    prettyDiffTime(
                        startTime,
                        finishTime
                    )
                }, #${actualTokenMatches.size}"
            )
            println(actualTokenMatches.size)
        }
    }

    fun searchOneTokenAfterIndex() {
        val token = "class"
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLogging(folder)
        val actualTokenMatches = searchApi.syncPerformSearchWithLogging(folder, token)
        val finishTime = LocalDateTime.now()
        println("total time: ${prettyDiffTime(startTime, finishTime)}")
        println(actualTokenMatches.size)
    }


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

