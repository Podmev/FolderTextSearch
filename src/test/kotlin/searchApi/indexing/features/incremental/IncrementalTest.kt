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
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Checking incremental indexing usage
 * */
class IncrementalTest {
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
     * Checking have updates on files when using incremental indexing if indexed folder changed.
     * Changes happened in different inner folder
     * */
    @Test
    fun incrementalDifferentInnerFolderThenHaveUpdatesTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"

        val file1: File = indexFolder.resolve("aaa").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText(text)

        searchApi.startIncrementalIndexing()
        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        val file2: File = indexFolder.resolve("bbb").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText(text)
        file1.writeText(text + text)
        TimeUnit.MILLISECONDS.sleep(100)

        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        searchApi.stopIncrementalIndexing()

        Assertions.assertNotEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the different result with incremental indexing"
        )
    }

    /**
     * Checking have updates on files when using incremental indexing if indexed folder changed.
     * Changes happened in same files
     * */
    @Test
    fun incrementalSameInnerFolderNewFileThenHaveUpdatesTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"
        val innerFolder = indexFolder.resolve("aaa")

        val file1: File = innerFolder.resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText(text)

        searchApi.startIncrementalIndexing()
        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        val file2: File = innerFolder.resolve("b.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText(text)
        TimeUnit.MILLISECONDS.sleep(100)

        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        searchApi.stopIncrementalIndexing()

        Assertions.assertNotEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the different result with incremental indexing"
        )
    }

}