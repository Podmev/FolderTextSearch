package impl.trigram

import kotlinx.coroutines.channels.Channel
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class TrigramIndexingContext(
    val folderPath: Path,
    val indexingState: TrigramIndexingState,
    val resultPathQueue: Queue<Path>,
    val trigramMap: TrigramMap
) {

    val visitedFilesNumber = AtomicLong(0L)
    val indexedFilesNumber = AtomicLong(0L)

    val visitedPathChannel = Channel<Path>()
    val indexedPathChannel = Channel<Path>()
    val tripletInPathChannel = Channel<Pair<String, Path>>()

}