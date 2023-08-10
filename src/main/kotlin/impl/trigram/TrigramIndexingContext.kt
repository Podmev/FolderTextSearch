package impl.trigram

import kotlinx.coroutines.channels.Channel
import java.nio.file.Path
import java.util.*

/**
 * Context used in indexing with all necessary components.
 * */
internal class TrigramIndexingContext(
    /**
     * Folder path for indexing.
     * */
    val folderPath: Path,
    /**
     * Indexing state, contains mutable state.
     * */
    val indexingState: TrigramIndexingState,
    /**
     * Queue of indexed file paths, used for result.
     * */
    val resultPathQueue: Queue<Path>,
    /**
     * Internal structure for index.
     * */
    val trigramMap: TrigramMap
) {

    /**
     * Unlimited capacity is used to achieve different independent speed of indexing parts:
     * walking files, parsing files, saving triplets
     * */
    private val channelCapacity = Channel.UNLIMITED
    val visitedPathChannel = Channel<Path>(channelCapacity)
    val indexedPathChannel = Channel<Path>(channelCapacity)
    val tripletInPathChannel = Channel<Pair<String, Path>>(channelCapacity)

}