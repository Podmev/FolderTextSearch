package api

interface SearchingState {
    fun isFinished(): Boolean
    fun cancel()
    fun getProgress(): Double
    fun getResult(): SearchResult
}