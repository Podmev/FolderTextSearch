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
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Checking incremental indexing usage
 * */
class IncrementalTest {
    private val commonFolder: Path = commonSetup.commonPath
    private val indexFolder: Path = commonFolder.resolve("tempFolder${UUID.randomUUID()}")
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
     * Checking to have updates on files when using incremental indexing if indexed folder changed.
     * Changes happened in different inner folder
     * */
    @Test
    fun differentInnerFolderThenHaveUpdatesTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"

        val file1: File = indexFolder.resolve("aaa").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText(text)

        searchApi.startIncrementalIndexing()
        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        val file2: File = indexFolder.resolve("bbb").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
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

    /**
     * Checking to have updates on files when using incremental indexing if indexed folder changed.
     * Changes happened in same inner folder
     * */
    @Test
    fun sameInnerFolderNewFileThenHaveUpdatesTest() {
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

    /**
     * Checking to have updates on files when using incremental indexing,
     * if firstly was performed index, then folder changed and only after started incremental indexing.
     * Changes happened in different inner folder
     * */
    @Test
    fun differentInnerFolderIncrementalIndexingStartsAfterIndexAndChangeThenHaveUpdatesTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"

        val file1: File = indexFolder.resolve("aaa").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText(text)

        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        val file2: File = indexFolder.resolve("bbb").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText(text)
        TimeUnit.MILLISECONDS.sleep(100)

        searchApi.startIncrementalIndexing()
        TimeUnit.MILLISECONDS.sleep(100)

        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)
        searchApi.stopIncrementalIndexing()

        Assertions.assertNotEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the different result with incremental indexing even staring after changes"
        )
    }

    /**
     * Checking to have updates on files when using incremental indexing,
     * if firstly was performed index, then folder changed and only after started incremental indexing.
     * Changes happened in same inner folder
     * */
    @Test
    fun sameInnerFolderIncrementalIndexingStartsAfterIndexAndChangeThenHaveUpdatesTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"

        val sameInnerFolder = indexFolder.resolve("aaa")
        val file1: File = sameInnerFolder.resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText(text)

        searchApi.syncPerformIndex(indexFolder)
        val tokensBeforeChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)

        val file2: File = sameInnerFolder.resolve("b.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText(text)
        TimeUnit.MILLISECONDS.sleep(100)

        searchApi.startIncrementalIndexing()
        TimeUnit.MILLISECONDS.sleep(100)

        val tokensAfterChange: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)
        searchApi.stopIncrementalIndexing()

        Assertions.assertNotEquals(
            tokensBeforeChange,
            tokensAfterChange,
            "Search gives the different result with incremental indexing even staring after changes"
        )
    }


    /**
     * Checking that it works to use incremental indexing 2 times
     * Checking to have updates on files when using incremental indexing if indexed folder changed.
     * And then after stopping incremental indexing turns incremental index again and add one more file with token.
     * Changes happened in same inner folder
     * */
    @Test
    fun twoSessionsTest() {
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
        TimeUnit.MILLISECONDS.sleep(100)


        searchApi.startIncrementalIndexing()
        TimeUnit.MILLISECONDS.sleep(100)
        val file3: File = innerFolder.resolve("c.txt").toFile().also { it.parentFile.mkdirs() }
        file3.writeText(text)
        TimeUnit.MILLISECONDS.sleep(100)

        val tokensAfter2Change: List<TokenMatch> = searchApi.syncSearchToken(indexFolder, commonToken)
        searchApi.stopIncrementalIndexing()


        Assertions.assertAll(
            { Assertions.assertEquals(1, tokensBeforeChange.size, "found 1 token before change") },
            { Assertions.assertEquals(2, tokensAfterChange.size, "found 2 token before change") },
            { Assertions.assertEquals(3, tokensAfter2Change.size, "found 3 token before change") },
        )
    }

    /**
     * Checking that it works to use incremental indexing when there 2 indices - for 2 different folders
     * Changes happened in same inner folder
     * */
    @Test
    fun twoFoldersTest() {
        val searchApi = searchApiGenerator()
        val text = "abcdefg"
        val indexFolder1 = indexFolder.resolve("temp1")
        val indexFolder2 = indexFolder.resolve("temp2")
        val innerFolder1 = indexFolder1.resolve("aaa")
        val innerFolder2 = indexFolder2.resolve("bbb")

        val file11: File = innerFolder1.resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file11.writeText(text)
        val file12: File = innerFolder2.resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file12.writeText(text)

        searchApi.startIncrementalIndexing()
        searchApi.syncPerformIndex(indexFolder1)
        searchApi.syncPerformIndex(indexFolder2)
        val tokensBeforeChange1: List<TokenMatch> = searchApi.syncSearchToken(indexFolder1, commonToken)
        val tokensBeforeChange2: List<TokenMatch> = searchApi.syncSearchToken(indexFolder2, commonToken)

        val file21: File = innerFolder1.resolve("b.txt").toFile().also { it.parentFile.mkdirs() }
        file21.writeText(text)
        val file22: File = innerFolder2.resolve("b.txt").toFile().also { it.parentFile.mkdirs() }
        file22.writeText(text)
        TimeUnit.MILLISECONDS.sleep(100)

        val tokensAfterChange1: List<TokenMatch> = searchApi.syncSearchToken(indexFolder1, commonToken)
        val tokensAfterChange2: List<TokenMatch> = searchApi.syncSearchToken(indexFolder2, commonToken)

        searchApi.stopIncrementalIndexing()

        Assertions.assertAll(
            {
                Assertions.assertNotEquals(
                    tokensBeforeChange1,
                    tokensAfterChange1,
                    "Search gives the different result with incremental indexing even staring after changes for folder1"
                )
            },
            {
                Assertions.assertNotEquals(
                    tokensBeforeChange2,
                    tokensAfterChange2,
                    "Search gives the different result with incremental indexing even staring after changes for folder 2"
                )
            }
        )
    }

}