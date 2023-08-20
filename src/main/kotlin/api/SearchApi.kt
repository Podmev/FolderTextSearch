package api

import java.nio.file.Path

/**
 * Main Api to search string (token) in all files of the folder
 * It has 2 steps: indexing and exactly searching token
 * Also it has api for checking index and remove it partly of full
 * */
interface SearchApi {
    /**
     * Creates and saves index for folder. It will be used for searching
     * */
    fun createIndexAtFolder(folderPath: Path): IndexingState

    /**
     *  Searches token in the folder with using created index.
     *  Restrictions for token:
     *  - at least 3 characters
     *  - no breaking line characters
     * */
    fun searchString(
        folderPath: Path,
        token: String
    ): SearchingState

    /**
     * Index folder and then search string
     * */
    fun indexAndSearchString(
        folderPath: Path,
        token: String
    ): IndexingAndSearchingState

    /**
     * Starts background incremental indexing
     *
     * @param [withInitialUpdate] - set false if you don't need to update all folders on start
     */
    fun startIncrementalIndexing(withInitialUpdate: Boolean = true): Boolean

    /**
     * Stops background incremental indexing
     */
    fun stopIncrementalIndexing(): Boolean

    /**
     * Checks if exists already index for folder
     * On fresh start should be false
     * After successful indexing at folder, it should be true normally
     * */
    fun hasIndexAtFolder(folderPath: Path): Boolean

    /**
     * Removes index at folder
     * */
    fun removeIndexAtFolder(folderPath: Path): Boolean

    /**
     * Removes full index. If there is index for several folders, they all will be removed
     * */
    fun removeFullIndex()

    /**
     * Returns all folders, where index is calculated
     * */
    fun getAllIndexedFolders(): List<Path>
}

/**
 * No index in Search at the moment
 * */
fun SearchApi.isIndexEmpty() = getAllIndexedFolders().isEmpty()