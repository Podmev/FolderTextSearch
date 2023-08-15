package api.tools.searchapi.search

import api.SearchApi
import api.TokenMatch
import java.nio.file.Path

/**
 * Util function to calculate index for folder, then after it is done, perform search for token
 * Used in tests
 * */
fun SearchApi.syncSearchToken(folderPath: Path, token: String): List<TokenMatch> {
    val searchingState = searchString(folderPath, token)
    return searchingState.result.get()
}