package api.tools.searchapi

import api.IndexingState
import api.SearchApi
import java.nio.file.Path

/**
 * Util function to calculate index for folder, then after it is done returns
 * Used in tests
 * */
fun SearchApi.syncPerformIndex(folderPathString: Path): IndexingState {
    val indexingState = createIndexAtFolder(folderPathString)
    indexingState.result.get()
    assert(indexingState.finished)
    return indexingState
}