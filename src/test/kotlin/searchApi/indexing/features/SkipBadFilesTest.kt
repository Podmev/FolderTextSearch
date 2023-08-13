package searchApi.indexing.features

import api.SearchApi
import api.tools.searchapi.syncPerformIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Checking that indexing doesn't break on files which are not supposed to be used in index.
 * They should be skipped and don't break indexing
 * */
class SkipBadFilesTest {
    private val badFilesFolder: Path = commonSetup.commonPath.resolve("badFiles")

    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * Check indexing folder with all collection of folders with bad files. Nothing should break
     * */
    @Test
    fun fullBadFilesFolderTest() {
        val folder = badFilesFolder
        val searchApi = searchApiGenerator()
        val state = searchApi.syncPerformIndex(folder)
        Assertions.assertAll(
            { Assertions.assertTrue(searchApi.hasIndexAtFolder(folder), "SearchApi has index for folder") },
            { Assertions.assertTrue(state.result.get().isNotEmpty(), "Result indexed files are not empty") }
        )
    }

    /**
     * Check indexing folder with separate folder with bad files with unique property: extension, encoding, etc.
     * Nothing should break
     * */
    @ParameterizedTest(name = "separateBadFilesFolderTest {0}")
    @MethodSource("badFilesFolderNamesProvider")
    fun separateBadFilesFolderTest(folderName: String) {
        val folder = badFilesFolder.resolve(folderName)
        val searchApi = searchApiGenerator()
        val state = searchApi.syncPerformIndex(folder)
        Assertions.assertAll(
            { Assertions.assertTrue(searchApi.hasIndexAtFolder(folder), "SearchApi has index for folder") },
            { Assertions.assertTrue(state.result.get().isNotEmpty(), "Result indexed files are not empty") }
        )
    }

    companion object {
        /**
         * Names of folder with bad data from parent folder "badFiles"
         * */
        private val badFilesFolderNamesList = listOf(
            "file7z",
            "fileBin",
            "fileClass",
            "fileDll",
            "fileExe",
            "fileJar",
            "fileJpg",
            "fileNoExtension",
            "filePng",
            "fileWithBadEncoding",
            "fileWithSomeLetterBadEncoding",
            "fileZip",
        )

        /**
         * Provides arguments for tests: separateBadFilesFolderTest
         * */
        @JvmStatic
        fun badFilesFolderNamesProvider(): Stream<Arguments> {
            return badFilesFolderNamesList
                .map { Arguments.of(it) }
                .stream()
        }
    }
}