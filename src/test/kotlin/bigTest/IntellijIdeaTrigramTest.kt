package bigTest

import api.SearchApi
import api.tools.syncSearchToken
import common.commonSetup
import trigram.TrigramSearchApi
import java.nio.file.Path
import java.time.LocalDateTime

/*Searching in intellij idea project*/
class IntellijIdeaTrigramTest {
    /*source code of intellij idea*/
    private val commonPath: Path = commonSetup.intellijIdeaProjectPath.resolve("java")

    private val searchApi: SearchApi = TrigramSearchApi()

    /*doesn't finish in 12 minutes. how long in fact I don't know
    * */
    fun indexWithSearchOneToken() {
        val token = "class"
        val folder = commonPath
        val startTime = LocalDateTime.now()
        println(startTime)
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

