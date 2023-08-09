package api.tools

import api.SearchApi
import api.TokenMatch
import java.nio.file.Path

/**
 * Util function to calculate index for folder, then after it is done, perform search for token
 * Used in tests
 * */
fun SearchApi.syncSearchTokenAfterIndex(folderPathString: Path, token: String): List<TokenMatch> {
    val indexingState = createIndexAtFolder(folderPathString)
    indexingState.result.get()!!
    assert(indexingState.finished)
    val searchingState = searchString(folderPathString, token)
    return searchingState.result.get()
}