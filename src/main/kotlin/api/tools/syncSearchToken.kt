package api.tools

import api.SearchApi
import api.TokenMatch
import java.nio.file.Path

/**
 * Util function to calculate index for folder, then after it is done, perform search for token
 * Used in tests
 * */
fun SearchApi.syncSearchToken(folderPathString: Path, token: String): List<TokenMatch> {
    val searchingState = searchString(folderPathString, token)
    return searchingState.result.get()
}