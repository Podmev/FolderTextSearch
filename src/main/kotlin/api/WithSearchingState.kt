package api

/**
 * Interface adds access to inner searchingState
 * */
interface WithSearchingState {

    /**
     * Inner searching state inside aggregated state
     * */
    val searchingState: SearchingState
}