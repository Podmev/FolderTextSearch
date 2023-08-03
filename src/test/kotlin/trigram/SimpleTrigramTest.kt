package trigram

import api.TokenMatch
import common.assertEqualsTokenMatches
import common.commonSetup
import common.syncSearchToken
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.nio.file.Path

class SimpleTrigramTest {
    private val commonPath: Path = commonSetup.commonPath
    /*using not by interface, because we use methods exactly from TrigramSearchApi*/
    private val searchApi: TrigramSearchApi = TrigramSearchApi()

    //TODO separate test in two
    /*Folder with 10 files, only 3 of them have match*/
    @Test
    fun tenFilesAndHasMatchTest() {
        val folderName = "tenFiles"
        val token = "fgh"
        val folderPath = commonPath.resolve(folderName)
        val actualTokenMatches = syncSearchToken(searchApi, folderPath, token)
        //TODO add util function to compare indexMap
        assertAll(
            { ->
                assertEqualsTokenMatches(
                    expectedTokenMatches = listOf(
                        TokenMatch(folderPath.resolve("4.txt"), 0L, 2L),
                        TokenMatch(folderPath.resolve("5.txt"), 0L, 1L),
                        TokenMatch(folderPath.resolve("6.txt"), 0L, 0L)
                    ),
                    actualTokenMatches = actualTokenMatches
                )
            },
            {-> Assertions.assertEquals(
                mapOf(
                    Pair("abc", setOf<Path>(folderPath.resolve("1.txt"))),
                    Pair("bcd", setOf<Path>(folderPath.resolve("1.txt"), folderPath.resolve("2.txt"))),
                    Pair("cde", setOf<Path>(folderPath.resolve("1.txt"), folderPath.resolve("2.txt"), folderPath.resolve("3.txt"))),
                    Pair("def", setOf<Path>(folderPath.resolve("2.txt"), folderPath.resolve("3.txt"), folderPath.resolve("4.txt"))),
                    Pair("efg", setOf<Path>(folderPath.resolve("3.txt"), folderPath.resolve("4.txt"), folderPath.resolve("5.txt"))),
                    Pair("fgh", setOf<Path>(folderPath.resolve("4.txt"), folderPath.resolve("5.txt"), folderPath.resolve("6.txt"))),
                    Pair("ghi", setOf<Path>(folderPath.resolve("5.txt"), folderPath.resolve("6.txt"), folderPath.resolve("7.txt"))),
                    Pair("hij", setOf<Path>(folderPath.resolve("6.txt"), folderPath.resolve("7.txt"), folderPath.resolve("8.txt"))),
                    Pair("ijk", setOf<Path>(folderPath.resolve("7.txt"), folderPath.resolve("8.txt"), folderPath.resolve("9.txt"))),
                    Pair("jkl", setOf<Path>(folderPath.resolve("8.txt"), folderPath.resolve("9.txt"), folderPath.resolve("10.txt"))),
                    Pair("klm", setOf<Path>(folderPath.resolve("9.txt"), folderPath.resolve("10.txt"))),
                    Pair("lmn", setOf<Path>(folderPath.resolve("10.txt"))),
                ),
                searchApi.getTrigramImmutableMap(folderPath)
            )}
        )
    }

}