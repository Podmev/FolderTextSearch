package searchApi.indexing.features.incremental

import api.SearchApi
import api.TokenMatch
import api.tools.searchapi.index.syncPerformIndex
import api.tools.searchapi.search.syncSearchToken
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup
import java.io.File
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Checking no incremental indexing usage
 * */
class NoIncrementalTest {
    private val commonFolder: Path = commonSetup.commonPath
    private val indexFolder: Path = commonFolder.resolve("tempFolder")
    private val commonToken: String = "abc"

    @BeforeTest
    fun init() {
        indexFolder.toFile().mkdir()
    }

    @AfterTest
    fun finalize() {
        val file = indexFolder.toFile()
        if (file.exists()) {
            file.deleteRecursively()
        }
    }

    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * Checking updates on files if indexed folder changed, but in the same file, so we still receive updates
     * */
    @Test
    fun noIncrementalSameFileThenStillHaveUpdatesTest() {
        val searchApi = searchApiGenerator()
        val file: File = indexFolder.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        val text = "abcdefg"
        file.writeText(text)
        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)
        file.writeText(text + text)
        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)
        Assertions.assertNotEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the different result without reindexing or incremental indexing, but only if changes happened same files"
        )
    }

    /**
     * Checking updates on files but in the same file if indexed folder changed, and then index was deleted and calculated again
     * */
    @Test
    fun noIncrementalSameFileThenUpdateOnReindexTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"

        val file: File = indexFolder.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file.writeText(text)

        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        file.writeText(text + text)

        searchApi.removeIndexAtFolder(indexFolder)
        searchApi.syncPerformIndex(indexFolder)
        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        Assertions.assertNotEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the different result on reindexing"
        )
    }

    /**
     * Checking no updates on files if indexed folder changed. Changes happened in different files
     * */
    @Test
    fun noIncrementalThenNoUpdatesTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"

        val file1: File = indexFolder.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText(text)

        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        val file2: File = indexFolder.resolve("b").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText(text)

        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)
        Assertions.assertEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the same result with reindexing or incremental indexing"
        )
    }

    /**
     * Checking updates on files if indexed folder changed, and then index was deleted and calculated again
     * */
    @Test
    fun noIncrementalButUpdateOnReindexTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"
        val file1: File = indexFolder.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText(text)

        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        val file2: File = indexFolder.resolve("b").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText(text)

        searchApi.removeIndexAtFolder(indexFolder)
        searchApi.syncPerformIndex(indexFolder)
        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        Assertions.assertNotEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the different result on reindexing"
        )
    }

//    /**
//     * Checking incremental indexing works, when we run it after normal indexing one folder
//     * */
//    @Test
//    fun incrementalAfterIndexTest() {
//        val searchApi = searchApiGenerator()
//        val started = searchApi.startIncrementalIndexing()
//        TimeUnit.MILLISECONDS.sleep(500)
//        val stopped = searchApi.stopIncrementalIndexing()
//        assertAll(
//            { Assertions.assertTrue(started, "Can start incremental indexing") },
//            { Assertions.assertTrue(stopped, "Can stop incremental indexing") }
//        )
//    }


}