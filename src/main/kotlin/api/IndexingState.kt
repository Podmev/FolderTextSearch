package api

interface IndexingState {
    val finished: Boolean
    /*can be from 0 till 1 inclusive borders, where 0 means not started, and 1  - finished*/
    val progress: Double
    fun cancel()
}