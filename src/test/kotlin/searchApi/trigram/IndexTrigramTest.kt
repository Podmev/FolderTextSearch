package searchApi.trigram

import api.tools.searchapi.syncPerformIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Test
import searchApi.common.assertEqualsTrigramMap
import searchApi.common.CommonSetup
import java.nio.file.Path

/**
 * Checking creating index for TrigramSearchApi
 * Using generator for SearchApi to have fresh state in SearchApi
 * */
class IndexTrigramTest {
    private val commonPath: Path = CommonSetup.commonPath

    /*using not by interface, because we use methods exactly from TrigramSearchApi* */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /*Folder with single file with single line* */
    @Test
    fun singleFileTest() {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val folder = commonPath.resolve(folderName)
        searchApi.syncPerformIndex(folder)
        assertEqualsTrigramMap(
            mapOf(
                Pair("abc", setOf(folder.resolve("a.txt"))),
                Pair("bcd", setOf(folder.resolve("a.txt"))),
                Pair("cde", setOf(folder.resolve("a.txt"))),
                Pair("def", setOf(folder.resolve("a.txt"))),
                Pair("efg", setOf(folder.resolve("a.txt"))),
            ),
            searchApi.getTrigramImmutableMap(folder)
        )
    }

    /*Folder with file and inner folder with file. Both files have single line, which contains token 1 time* */
    @Test
    fun fileAndFolderWithFileTest() {
        val searchApi = searchApiGenerator()
        val folderName = "fileAndFolderWithFile"
        val folder = commonPath.resolve(folderName)
        searchApi.syncPerformIndex(folder)
        assertEqualsTrigramMap(
            mapOf(
                Pair("abc", setOf(folder.resolve("a.txt"))),
                Pair("bcd", setOf(folder.resolve("a.txt"))),
                Pair("cde", setOf(folder.resolve("a.txt"), folder.resolve("b").resolve("c.txt"))),
                Pair("def", setOf(folder.resolve("b").resolve("c.txt"))),
                Pair("efg", setOf(folder.resolve("b").resolve("c.txt"))),
            ),
            searchApi.getTrigramImmutableMap(folder)
        )
    }

    /*Folder with single file with 3 lines, there are repeats of same tokens in one file* */
    @Test
    fun fileWithMatchesOnDifferentLinesTest() {
        val searchApi = searchApiGenerator()
        val folderName = "fileWithMatchesOnDifferentLines"
        val folder = commonPath.resolve(folderName)
        searchApi.syncPerformIndex(folder)
        assertEqualsTrigramMap(
            mapOf(
                Pair("abc", setOf(folder.resolve("a.txt"))),
                Pair("bcd", setOf(folder.resolve("a.txt"))),
                Pair("cde", setOf(folder.resolve("a.txt"))),
                Pair("def", setOf(folder.resolve("a.txt"))),
                Pair("efg", setOf(folder.resolve("a.txt"))),
                Pair("fgh", setOf(folder.resolve("a.txt"))),
                Pair("ghi", setOf(folder.resolve("a.txt"))),
                Pair("hij", setOf(folder.resolve("a.txt"))),
                Pair("ijk", setOf(folder.resolve("a.txt"))),
                Pair("jkl", setOf(folder.resolve("a.txt"))),
                Pair("klm", setOf(folder.resolve("a.txt"))),
                Pair("lmn", setOf(folder.resolve("a.txt"))),
                Pair("mno", setOf(folder.resolve("a.txt"))),
                Pair("nop", setOf(folder.resolve("a.txt"))),
                Pair("opq", setOf(folder.resolve("a.txt")))
            ),
            searchApi.getTrigramImmutableMap(folder)
        )
    }

    /*Folder with 2 files* */
    @Test
    fun twoFilesOneMatchTest() {
        val searchApi = searchApiGenerator()
        val folderName = "twoFilesOneMatch"
        val folder = commonPath.resolve(folderName)
        searchApi.syncPerformIndex(folder)
        assertEqualsTrigramMap(
            mapOf(
                Pair("abc", setOf(folder.resolve("a.txt"))),
                Pair("bcd", setOf(folder.resolve("a.txt"))),
                Pair("cde", setOf(folder.resolve("a.txt"))),
                Pair("def", setOf(folder.resolve("a.txt"))),
                Pair("efg", setOf(folder.resolve("a.txt"))),
                Pair("fgh", setOf(folder.resolve("a.txt"))),
                Pair("ghi", setOf(folder.resolve("a.txt"))),
                Pair("hij", setOf(folder.resolve("a.txt"))),

                Pair("klm", setOf(folder.resolve("b.txt"))),
                Pair("lmn", setOf(folder.resolve("b.txt"))),
                Pair("mno", setOf(folder.resolve("b.txt"))),
                Pair("nop", setOf(folder.resolve("b.txt"))),
                Pair("opq", setOf(folder.resolve("b.txt"))),
                Pair("pqr", setOf(folder.resolve("b.txt"))),
                Pair("qrs", setOf(folder.resolve("b.txt"))),
                Pair("rst", setOf(folder.resolve("b.txt"))),
            ),
            searchApi.getTrigramImmutableMap(folder)
        )
    }

    /*Folder with sequence of 10 inner folder with single file* */
    @Test
    fun deepFileTest() {
        val searchApi = searchApiGenerator()
        val folderName = "deepFile"
        val folder = commonPath.resolve(folderName)
        val innerPath = folder
            .resolve("1")
            .resolve("2")
            .resolve("3")
            .resolve("4")
            .resolve("5")
            .resolve("6")
            .resolve("7")
            .resolve("8")
            .resolve("9")
            .resolve("10")
            .resolve("a.txt")

        searchApi.syncPerformIndex(folder)
        assertEqualsTrigramMap(
            mapOf(
                Pair("abc", setOf(innerPath)),
                Pair("bcd", setOf(innerPath)),
                Pair("cde", setOf(innerPath)),
                Pair("def", setOf(innerPath)),
                Pair("efg", setOf(innerPath)),
                Pair("fgh", setOf(innerPath)),
                Pair("ghi", setOf(innerPath)),
                Pair("hij", setOf(innerPath)),
            ),
            searchApi.getTrigramImmutableMap(folder)
        )
    }

    /*Folder with 10 files* */
    @Test
    fun tenFilesAndHasMatchTest() {
        val searchApi = searchApiGenerator()
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