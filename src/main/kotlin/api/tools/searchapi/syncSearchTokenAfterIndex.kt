package api.tools.searchapi

import api.SearchApi
import api.TokenMatch
import java.nio.file.Path

/**
 * Util function to calculate index for folder, then after it is done, perform search for token
 * Used in tests
 * */
fun SearchApi.syncSearchTokenAfterIndex(folderPath: Path, token: String): List<TokenMatch> {
    val indexingState = createIndexAtFolder(folderPath)
    indexingState.result.get()!!
    assert(indexingState.finished)
    val searchingState = searchString(folderPath, token)
    return searchingState.result.get()
}