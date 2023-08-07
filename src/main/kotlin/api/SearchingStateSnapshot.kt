package api

import java.nio.file.Path

/*Immutable state of searching*/
data class SearchingStateSnapshot(
    val finished: Boolean,
    val progress: Double,

    val visitedPathsBuffer: List<Path>,
    val tokenMatchesBuffer: List<TokenMatch>,

    val visitedFilesNumber: Long,
    val totalFilesNumber: Long?,

    val visitedFilesByteSize: Long,
    val parsedFilesByteSize: Long,
    val totalFilesByteSize: Long?

)
