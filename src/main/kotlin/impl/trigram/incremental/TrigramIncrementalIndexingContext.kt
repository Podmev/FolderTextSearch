package impl.trigram.incremental

import impl.trigram.map.TrigramMap
import kotlinx.coroutines.channels.Channel
import java.nio.file.Path

/**
 * Context used in indexing with all necessary components.
 * */
internal class TrigramIncrementalIndexingContext(
    /**
     * Internal structure for index by folder.
     * */
    val trigramMapByFolder: Map<Path, TrigramMap>
) {

    /**
     * Unlimited capacity is used to achieve different independent speed of indexing parts:
     * walking files, parsing files, saving triplets
     * */
    private val channelCapacity = Channel.UNLIMITED
    val changedFileChannel: Channel<FileEvent> = Channel(channelCapacity)
}