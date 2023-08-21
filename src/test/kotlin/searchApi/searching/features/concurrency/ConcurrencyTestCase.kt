package searchApi.searching.features.concurrency

import api.SearchApi
import api.SearchingState
import api.tools.searchapi.search.syncPerformSearch
import searchApi.common.commonSetup
import java.nio.file.Path

/**
 * Concurrency test cases for parametrized tests
 * */
data class ConcurrencyTestCase(
    val timing: Timing,
    val tokenCombination: TokenCombination,
    val numberOfInstances: NumberOfInstances,
    val folderCombination: FolderCombination
)

/**
 * Relation second folder to first
 * */
enum class FolderCombination(val folder1: Path, val folder2: Path) {
    SAME_FOLDER(commonSetup.srcFolder, commonSetup.srcFolder),
    SUB_FOLDER(commonSetup.srcFolder, commonSetup.srcFolder.resolve("main")),
    PARENT_FOLDER(commonSetup.srcFolder.resolve("main"), commonSetup.srcFolder),
    DIFFERENT_FOLDER(commonSetup.srcFolder.resolve("main"), commonSetup.srcFolder.resolve("test"))
}

/**
 * Number of instances of SearchApi in use
 * */
enum class NumberOfInstances {
    /**
     * Both indices are calculated via same instance of SearchApi
     * */
    SINGLE,

    /**
     * Both indices are calculated via separate instances of SearchApi
     * */
    DUAL
}

/**
 * Combination of tokens
 * */
enum class TokenCombination(val token1: String, val token2: String) {
    SAME_TOKEN("index", "index"),
    DIFFERENT_TOKEN("index", "search")
}

/**
 * Depends on timing synchronous or asynchronous
 * */
enum class Timing {
    /**
     * Using asynchronous api
     * */
    CONCURRENT {
        override fun makeSearch(searchApi: SearchApi, folder: Path, token: String): SearchingState {
            return searchApi.searchString(folder, token)
        }
    },

    /**
     * Using synchronous api
     * */
    SEQUENCIAL {
        override fun makeSearch(searchApi: SearchApi, folder: Path, token: String): SearchingState {
            return searchApi.syncPerformSearch(folder, token)
        }
    };

    abstract fun makeSearch(searchApi: SearchApi, folder: Path, token: String): SearchingState

}


