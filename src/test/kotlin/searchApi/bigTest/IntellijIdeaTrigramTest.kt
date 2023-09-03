package searchApi.bigTest

import api.SearchApi
import api.TokenMatch
import api.tools.searchapi.index.syncPerformIndexWithLogging
import api.tools.searchapi.index.syncPerformIndexWithLoggingAndCancel
import api.tools.searchapi.search.syncPerformSearchWithLogging
import api.tools.searchapi.search.syncSearchToken
import impl.trigram.TrigramSearchApi
import impl.trigram.map.TrigramMapType
import searchApi.common.commonSetup
import utils.prettyDiffTime
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*
import kotlin.io.path.absolute
import kotlin.io.path.readLines

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
        searchApi.syncPerformIndexWithLoggingAndCancel(folderPath = folder, cancelAtProgress = 0.00)
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
        val token = "volatile"
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLogging(folder)
        val actualTokenMatches = searchApi.syncPerformSearchWithLogging(folder, token)
        val finishTime = LocalDateTime.now()
        println("total time: ${prettyDiffTime(startTime, finishTime)}")
        println(actualTokenMatches.size)
    }

    fun searchOneTokenAfterIndexGrepFormat() {
        val token = "volatile"
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
        searchApi.syncPerformIndexWithLogging(folder)
        val actualTokenMatches = searchApi.syncPerformSearchWithLogging(folder, token)
        val finishTime = LocalDateTime.now()
        println("total time: ${prettyDiffTime(startTime, finishTime)}")
        println(actualTokenMatches.size)
        val sortedTokenMatches: List<TokenMatch> = actualTokenMatches.sortedWith(
            compareBy({ it.filePath.toString().lowercase(Locale.getDefault()) },
                { it.line },
                { it.column })
        )
        val root = commonSetup.projectPath.absolute().parent
        for (tokenMatch in sortedTokenMatches) {
            val lines = tokenMatch.filePath.readLines()
            val line = lines[tokenMatch.line.toInt() - 1]
            val relativePath = root.relativize(tokenMatch.filePath)
            val linuxPath = relativePath.toString().replace("\\", "/")
            println("$linuxPath:$line")
        }
    }

    fun searchOneTokenAfterIndexWithSimpleTrigramMap() {
        val searchApi: SearchApi = TrigramSearchApi(TrigramMapType.SIMPLE)
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



