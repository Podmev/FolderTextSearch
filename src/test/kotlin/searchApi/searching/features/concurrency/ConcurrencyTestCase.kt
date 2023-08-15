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
enum class FolderCombination {
    SAME_FOLDER {
        override val folder1: Path = commonSetup.srcFolder
        override val folder2: Path = commonSetup.srcFolder
    },
    SUB_FOLDER {
        override val folder1: Path = commonSetup.srcFolder
        override val folder2: Path = commonSetup.srcFolder.resolve("main")
    },
    PARENT_FOLDER {
        override val folder1: Path = commonSetup.srcFolder.resolve("main")
        override val folder2: Path = commonSetup.srcFolder
    },
    DIFFERENT_FOLDER {
        override val folder1: Path = commonSetup.srcFolder.resolve("main")
        override val folder2: Path = commonSetup.srcFolder.resolve("test")
    };

    abstract val folder1: Path
    abstract val folder2: Path
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
enum class TokenCombination {
    SAME_TOKEN {
        override val token1: String = "index"
        override val token2: String = token1
    },
    DIFFERENT_TOKEN {
        override val token1: String = "index"
        override val token2: String = "search"
    };

    abstract val token1: String
    abstract val token2: String
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


