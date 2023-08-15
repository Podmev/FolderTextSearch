package api.tools.searchapi.indexAndSearch

import api.SearchApi
import api.TokenMatch
import java.nio.file.Path

/**
 * Util function to calculate index for folder and search for token
 * Used in tests
 * */
fun SearchApi.syncIndexAndSearchToken(folderPath: Path, token: String): List<TokenMatch> {
    val indexingAndSearchingState = indexAndSearchString(folderPath, token)
    return indexingAndSearchingState.result.get()
}