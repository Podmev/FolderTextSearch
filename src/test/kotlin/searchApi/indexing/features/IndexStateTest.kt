package searchApi.indexing.features

import api.SearchApi
import api.tools.searchapi.index.syncPerformIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup
import java.nio.file.Path

/**
 * testing SearchApi methods:
 *   fun hasIndexAtFolder(folderPath: Path): Boolean
 *   fun removeIndexAtFolder(folderPath: Path): Boolean
 *   fun removeFullIndex()
 *   fun getAllIndexedFolders(): List<Path>
 * */
class IndexStateTest {
    private val commonPath: Path = commonSetup.commonPath
    private val folderName1 = "singleFile"
    private val folderName2 = "fileAndFolderWithFile"
    private val deepFileFolderName = "deepFile"

    /**
     * Before calculating index at folder, search api doesn't have index for it.
     * */
    @Test
    fun hasIndexAtFolderFalseBeforeIndexTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        val hasIndexAtFolderBeforeIndexing = searchApi.hasIndexAtFolder(folder)
        searchApi.syncPerformIndex(folder)
        Assertions.assertFalse(hasIndexAtFolderBeforeIndexing)
    }

    /**
     * After calculating index at folder, search api saves it.
     * */
    @Test
    fun hasIndexAtFolderTrueAfterIndexTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        searchApi.syncPerformIndex(folder)
        Assertions.assertTrue(searchApi.hasIndexAtFolder(folder))
    }

    /**
     * After calculating index at folder and removing it, search api doesn't have it anymore.
     * */
    @Test
    fun hasIndexAtFolderFalseAfterRemovingIndexAtFolderTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        searchApi.syncPerformIndex(folder)
        searchApi.removeIndexAtFolder(folder)
        Assertions.assertFalse(searchApi.hasIndexAtFolder(folder))
    }

    /**
     * After calculating index at folder and removing it, search api doesn't have it anymore.
     * */
    @Test
    fun hasIndexAtFolderFalseAfterRemovingFullIndexTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        searchApi.syncPerformIndex(folder)
        searchApi.removeFullIndex()
        Assertions.assertFalse(searchApi.hasIndexAtFolder(folder))
    }

    /**
     * After calculating 2 index at 2 folders and removing both, search api doesn't have index at any folder.
     * */
    @Test
    fun hasIndexAtFolderFalseFor2FoldersAfterRemovingFullIndexTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder1 = commonPath.resolve(folderName1)
        val folder2 = commonPath.resolve(folderName2)
        searchApi.syncPerformIndex(folder1)
        searchApi.syncPerformIndex(folder2)
        searchApi.removeFullIndex()
        Assertions.assertAll(
            { Assertions.assertFalse(searchApi.hasIndexAtFolder(folder1)) },
            { Assertions.assertFalse(searchApi.hasIndexAtFolder(folder2)) }
        )
    }

    /**
     * After calculating 2 index at 2 folders, search api has index at both folders.
     * */
    @Test
    fun hasIndexAtFolderTrueFor2FoldersAfterIndexBothTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder1 = commonPath.resolve(folderName1)
        val folder2 = commonPath.resolve(folderName2)
        searchApi.syncPerformIndex(folder1)
        searchApi.syncPerformIndex(folder2)
        Assertions.assertAll(
            { Assertions.assertTrue(searchApi.hasIndexAtFolder(folder1)) },
            { Assertions.assertTrue(searchApi.hasIndexAtFolder(folder2)) }
        )
    }

    /**
     * After calculating 2 index at 2 folders, search api has index at both folders.
     * */
    @Test
    fun hasIndexAtFolderTrueAfterRepeatingIndexTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        searchApi.syncPerformIndex(folder)
        searchApi.syncPerformIndex(folder)
        Assertions.assertTrue(searchApi.hasIndexAtFolder(folder))
    }

    /**
     * Before calculating 1 index at folder, search api has empty list of indexed folders.
     * */
    @Test
    fun getAllIndexedFoldersEmptyBeforeIndexFolderTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        val allIndexedFoldersBeforeIndex = searchApi.getAllIndexedFolders()
        searchApi.syncPerformIndex(folder)
        Assertions.assertEquals(emptyList<Path>(), allIndexedFoldersBeforeIndex)
    }

    /**
     * After calculating 1 index at folder, search api has list of single indexed folder.
     * */
    @Test
    fun getAllIndexedFoldersAfterIndexFolderTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        searchApi.syncPerformIndex(folder)
        Assertions.assertEquals(listOf(folder), searchApi.getAllIndexedFolders())
    }

    /**
     * After calculating 2 index at 2 folders, search api has index at both folders.
     * Using set, because the is no order.
     * */
    @Test
    fun getAllIndexedFoldersFor2FoldersAfterIndexBothTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder1 = commonPath.resolve(folderName1)
        val folder2 = commonPath.resolve(folderName2)
        searchApi.syncPerformIndex(folder1)
        searchApi.syncPerformIndex(folder2)
        Assertions.assertEquals(setOf(folder1, folder2), searchApi.getAllIndexedFolders().toSet())
    }

    /**
     * After calculating 1 index at folder with many subfolders, search api has list of only single indexed folder,
     * but not subfolders.
     * */
    @Test
    fun getAllIndexedFoldersAfterIndexFolderWithManySubfoldersTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(deepFileFolderName)
        searchApi.syncPerformIndex(folder)
        Assertions.assertEquals(listOf(folder), searchApi.getAllIndexedFolders())
    }

    /**
     * After calculating 2 index at 2 folders and removing both, search api has empty list of indexed folders.
     * */
    @Test
    fun getAllIndexedFoldersEmptyFor2FoldersAfterRemovingFullIndexTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder1 = commonPath.resolve(folderName1)
        val folder2 = commonPath.resolve(folderName2)
        searchApi.syncPerformIndex(folder1)
        searchApi.syncPerformIndex(folder2)
        searchApi.removeFullIndex()
        Assertions.assertEquals(emptyList<Path>(), searchApi.getAllIndexedFolders())
    }

    /**
     * After calculating index at folder, when we remove it, we receive true - successfully removed.
     * */
    @Test
    fun removeIndexAtFolderTrueAfterIndexingFolderTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        searchApi.syncPerformIndex(folder)
        Assertions.assertTrue(searchApi.removeIndexAtFolder(folder))
    }

    /**
     * Before calculating index at folder, when we remove it, we receive false - failed to remove it.
     * */
    @Test
    fun removeIndexAtFolderTrueBeforeIndexingFolderTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        Assertions.assertFalse(searchApi.removeIndexAtFolder(folder))
        searchApi.syncPerformIndex(folder)
    }

    /**
     * After calculating index at folder and removing it, when we remove it again,
     * we receive false - failed to remove it.
     * */
    @Test
    fun removeIndexAtFolderFalseAfterOtherRemoveTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        searchApi.syncPerformIndex(folder)
        searchApi.removeIndexAtFolder(folder)
        Assertions.assertFalse(searchApi.removeIndexAtFolder(folder))
    }

    /**
     * Removing full before and after calculating index at folder, then checking list of indexed folders in 4 moments.
     * <list1>
     * removeFull
     * <list2>
     * indexing
     * <list3>
     * removeFull
     * <list4>
     * Only list3 would have single indexed folder, in other moments it will be empty.
     * */
    @Test
    fun removeFullIndexBeforeAndAfterIndexTest() {
        val searchApi: SearchApi = TrigramSearchApi()
        val folder = commonPath.resolve(folderName1)
        val indexedFoldersBeforeRemoveFull = searchApi.getAllIndexedFolders()
        searchApi.removeFullIndex()
        val indexedFoldersBeforeIndex = searchApi.getAllIndexedFolders()
        searchApi.syncPerformIndex(folder)
        val indexedFoldersAfterIndex = searchApi.getAllIndexedFolders()
        searchApi.removeFullIndex()
        val indexedFoldersAfterIndexAndRemoveFull = searchApi.getAllIndexedFolders()
        Assertions.assertAll(
            { Assertions.assertEquals(emptyList<Path>(), indexedFoldersBeforeRemoveFull) },
            { Assertions.assertEquals(emptyList<Path>(), indexedFoldersBeforeIndex) },
            { Assertions.assertEquals(listOf(folder), indexedFoldersAfterIndex) },
            { Assertions.assertEquals(emptyList<Path>(), indexedFoldersAfterIndexAndRemoveFull) },
        )
    }
}