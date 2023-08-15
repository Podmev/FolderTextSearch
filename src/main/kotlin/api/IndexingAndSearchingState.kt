package api

/**
 * Aggregated state of joined index and search operation
 * */
interface IndexingAndSearchingState : WithIndexingState, WithSearchingState, SearchingState

/**
 * Takes immutable snapshot of IndexingAndSearchingState
 * */
fun IndexingAndSearchingState.toSnapshot(): IndexingAndSearchingStateSnapshot =
    IndexingAndSearchingStateSnapshot(
        indexingStateSnapshot = indexingState.toSnapshot(),
        searchingStateSnapshot = searchingState.toSnapshot(),
        status = status,
        progress = progress,
        visitedPathsBuffer = getVisitedPathsBuffer(true),
        tokenMatchesBuffer = getTokenMatchesBuffer(true),
        tokenMatchesNumber = tokenMatchesNumber,
        visitedFilesNumber = visitedFilesNumber,
        totalFilesNumber = totalFilesNumber,
        visitedFilesByteSize = visitedFilesByteSize,
        parsedFilesByteSize = parsedFilesByteSize,
        totalFilesByteSize = totalFilesByteSize,
        startTime = startTime,
        lastWorkingTime = lastWorkingTime,
        totalTime = totalTime,
        failReason = failReason
    )