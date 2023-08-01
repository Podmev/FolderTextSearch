package api

interface SearchingState {
    val finished: Boolean
    /*can be from 0 till 1 inclusive borders, where 0 means not started, and 1  - finished*/
    val progress: Double
    val result: SearchResult
    fun cancel()
}