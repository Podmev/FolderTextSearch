package searchApi.indexing.features.concurrency

import api.IndexingState
import api.SearchApi
import api.tools.searchapi.index.syncPerformIndex
import searchApi.common.commonSetup
import java.nio.file.Path

/**
 * Concurrency test cases for parametrized tests
 * */
data class ConcurrencyTestCase(
    val timing: Timing,
    val indexPresence: IndexPresence,
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
 * Has of no index for each of 2 folders
 * */
enum class IndexPresence(val hasFolder1Index: Boolean, val hasFolder2Index: Boolean) {
    NO_INDEX(false, false),
    ONLY_FIRST(true, false),
    ONLY_SECOND(false, true),
    BOTH_INDICES(true, true)
}

/**
 * Depends on timing synchronous or asynchronous
 * */
enum class Timing {
    /**
     * Using asynchronous api
     * */
    CONCURRENT {
        override fun constructIndex(searchApi: SearchApi, folder: Path): IndexingState {
            return searchApi.createIndexAtFolder(folder)
        }
    },

    /**
     * Using synchronous api
     * */
    SEQUENCIAL {
        override fun constructIndex(searchApi: SearchApi, folder: Path): IndexingState {
            return searchApi.syncPerformIndex(folder)
        }
    };

    abstract fun constructIndex(searchApi: SearchApi, folder: Path): IndexingState

}


