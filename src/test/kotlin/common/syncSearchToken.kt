package common

import api.SearchApi
import api.TokenMatch
import java.nio.file.Path

/*Util function to calculate index for folder, then after it is done, perform search for token
* Used in tests
* */
fun syncSearchToken(searchApi: SearchApi, folderPathString: Path, token: String): List<TokenMatch> {
    val indexingState = searchApi.createIndexAtFolder(folderPathString)
    indexingState.result.get()!!
    assert(indexingState.finished)
    val searchingState = searchApi.searchString(folderPathString, token)
    return searchingState.result.get()
}