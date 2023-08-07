package api

import java.nio.file.Path

/*Immutable state of indexing*/
data class IndexingStateSnapshot(
    val finished: Boolean,
    val progress: Double,
    val visitedPathsBuffer: List<Path>,
    val indexedPathsBuffer: List<Path>,
    val visitedFilesNumber: Long,
    val indexedFilesNumber: Long,
    val totalFilesNumber: Long?
)
