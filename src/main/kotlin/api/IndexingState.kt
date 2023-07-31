package api

interface IndexingState {
    fun isFinished(): Boolean
    fun cancel()
    fun getProgress(): Double
}