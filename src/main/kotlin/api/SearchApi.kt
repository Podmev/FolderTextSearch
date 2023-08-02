package api

import java.nio.file.Path

/*Main Api to search string (token) in all files of the folder
 It has 2 steps: indexing and exactly searching token
* */
interface SearchApi {
    //TODO maybe we need sync and async api
    /*Creates and saves index for folder. It will be used for searching*/
    fun createIndexAtFolder(folderPath: Path): IndexingState

    /*Searches token in the folder with using created index.*/
    fun searchString(
        folderPath: Path,
        token: String,
        settings: SearchSettings = defaultSearchSettings
    ): SearchingState
}