package trigram

import api.TokenMatch
import common.assertEqualsTokenMatches
import common.assertEqualsTrigramMap
import common.CommonSetup
import common.syncSearchToken
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.nio.file.Path

class SimpleTrigramTest {
    private val commonPath: Path = CommonSetup.commonPath

    /*using not by interface, because we use methods exactly from TrigramSearchApi*/
    private val searchApi: TrigramSearchApi = TrigramSearchApi()

    //TODO separate test in two
    /*Folder with 10 files, only 3 of them have match*/
    @Test
    fun tenFilesAndHasMatchTest() {
        val folderName = "tenFiles"
        val token = "fgh"
        val folder = commonPath.resolve(folderName)
        val actualTokenMatches = syncSearchToken(searchApi, folder, token)
        assertAll(
            { ->
                assertEqualsTokenMatches(
                    expectedTokenMatches = listOf(
                        TokenMatch(folder.resolve("4.txt"), 0L, 2L),
                        TokenMatch(folder.resolve("5.txt"), 0L, 1L),
                        TokenMatch(folder.resolve("6.txt"), 0L, 0L)
                    ),
                    actualTokenMatches = actualTokenMatches
                )
            },
            { ->
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
        )
    }

}