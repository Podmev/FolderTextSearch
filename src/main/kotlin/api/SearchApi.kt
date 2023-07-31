package api

interface SearchApi {
    //TODO maybe we need sync and async api
    fun createIndexAtFolder(folderPath: String): IndexingState
    fun searchString(
        folderPath: String,
        token: String,
        settings: SearchSettings = defaultSearchSettings
    ): SearchingState
}