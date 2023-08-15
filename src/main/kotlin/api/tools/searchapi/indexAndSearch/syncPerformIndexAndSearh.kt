package api.tools.searchapi.indexAndSearch

import api.IndexingAndSearchingState
import api.SearchApi
import java.nio.file.Path

/**
 * Util function to calculate index for folder and search there together, then after it is done returns
 * Used in tests
 * */
fun SearchApi.syncPerformIndexAndSearch(folderPath: Path, token: String): IndexingAndSearchingState {
    val indexingAndSearchingState = indexAndSearchString(folderPath, token)
    indexingAndSearchingState.result.get()
    assert(indexingAndSearchingState.finished)
    return indexingAndSearchingState
}