package searchApi.trigram

import api.SearchApi
import api.TokenMatch
import api.tools.searchapi.syncPerformIndex
import api.tools.searchapi.syncSearchTokenAfterIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup
import searchApi.common.assertEqualsTokenMatches
import searchApi.common.assertEqualsTrigramMap
import java.nio.file.Path

/**
 * Smoke test for TrigramSearchApi.
 * Using generator for SearchApi to have fresh state in SearchApi.
 * */
class SimpleTrigramTest {
    private val commonPath: Path = commonSetup.commonPath

    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /*Folder with 10 files, only 3 of them have match* */
    @Test
    fun tenFilesHasMatchesTest() {
        val searchApi: SearchApi = searchApiGenerator()
        val folderName = "tenFiles"
        val token = "fgh"
        val folder = commonPath.resolve(folderName)
        val actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folder, token)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folder.resolve("4.txt"), 1L, 3L),
                TokenMatch(folder.resolve("5.txt"), 1L, 2L),
                TokenMatch(folder.resolve("6.txt"), 1L, 1L)
            ),
            actualTokenMatches = actualTokenMatches
        )
    }

    /**
     * Folder with 10 files, checking state of trigram Map
     * */
    @Test
    fun tenFilesTrigramMapTest() {
        val searchApi: TrigramSearchApi = searchApiGenerator()
        val folderName = "tenFiles"
        val folder = commonPath.resolve(folderName)
        searchApi.syncPerformIndex(folder)
        assertEqualsTrigramMap(
            mapOf(
                Pair("abc", setOf(folder.resolve("1.txt"))),
                Pair("bcd", setOf(folder.resolve("1.txt"), folder.resolve("2.txt"))),
                Pair("cde", setOf(folder.resolve("1.txt"), folder.resolve("2.txt"), folder.resolve("3.txt"))),
                Pair("def", setOf(folder.resolve("2.txt"), folder.resolve("3.txt"), folder.resolve("4.txt"))),
                Pair("efg", setOf(folder.resolve("3.txt"), folder.resolve("4.txt"), folder.resolve("5.txt"))),
                Pair("fgh", setOf(folder.resolve("4.txt"), folder.resolve("5.txt"), folder.resolve("6.txt"))),
                Pair("ghi", setOf(folder.resolve("5.txt"), folder.resolve("6.txt"), folder.resolve("7.txt"))),
                Pair("hij", setOf(folder.resolve("6.txt"), folder.resolve("7.txt"), folder.resolve("8.txt"))),
                Pair("ijk", setOf(folder.resolve("7.txt"), folder.resolve("8.txt"), folder.resolve("9.txt"))),
                Pair("jkl", setOf(folder.resolve("8.txt"), folder.resolve("9.txt"), folder.resolve("10.txt"))),
                Pair("klm", setOf(folder.resolve("9.txt"), folder.resolve("10.txt"))),
                Pair("lmn", setOf(folder.resolve("10.txt"))),
            ),
            searchApi.getTrigramImmutableMap(folder)
        )
    }

}