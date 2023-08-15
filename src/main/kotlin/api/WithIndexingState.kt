package api

/**
 * Interface adds access to inner indexingState
 * */
interface WithIndexingState {
    /**
     * Inner indexing state inside aggregated state
     * */
    val indexingState: IndexingState
}