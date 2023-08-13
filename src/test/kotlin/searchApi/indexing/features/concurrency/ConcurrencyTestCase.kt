package searchApi.indexing.features.concurrency

import api.IndexingState
import api.SearchApi
import api.tools.searchapi.syncPerformIndex
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
) {

}

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
 * Has of no index for each of 2 folders
 * */
enum class IndexPresence {
    NO_INDEX {
        override val hasFolder1Index: Boolean = false
        override val hasFolder2Index: Boolean = false
    },
    ONLY_FIRST {
        override val hasFolder1Index: Boolean = true
        override val hasFolder2Index: Boolean = false
    },
    ONLY_SECOND {
        override val hasFolder1Index: Boolean = false
        override val hasFolder2Index: Boolean = true
    },
    BOTH_INDICES {
        override val hasFolder1Index: Boolean = true
        override val hasFolder2Index: Boolean = true
    };

    abstract val hasFolder1Index: Boolean
    abstract val hasFolder2Index: Boolean
}

/**
 * Depends on timing synchronous or asynchronous
 * */
enum class Timing {
    CONCURRENT {
        override fun constructIndex(searchApi: SearchApi, folder: Path) =
            searchApi.createIndexAtFolder(folder)
    },
    SEQUENCIAL {
        override fun constructIndex(searchApi: SearchApi, folder: Path) =
            searchApi.syncPerformIndex(folder)
    };

    abstract fun constructIndex(searchApi: SearchApi, folder: Path): IndexingState

}


