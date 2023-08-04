package common

import api.SearchApi
import java.nio.file.Path

/*Util function to calculate index for folder, then after it is done returns
* Used in tests
* */
fun syncPerformIndex(searchApi: SearchApi, folderPathString: Path) {
    val indexingState = searchApi.createIndexAtFolder(folderPathString)
    indexingState.result.get()!!
    assert(indexingState.finished)
}