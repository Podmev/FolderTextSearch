package api.tools.searchapi

import api.SearchApi
import api.SearchingState
import java.nio.file.Path

/**
 * Util function to calculate search token in folder, then after it is done returns
 * Used in tests
 * */
fun SearchApi.syncPerformSearch(folderPath: Path, token: String): SearchingState {
    val searchingState = searchString(folderPath, token)
    searchingState.result.get()
    assert(searchingState.finished)
    return searchingState
}