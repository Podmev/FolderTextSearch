package impl.trigram

import kotlinx.coroutines.channels.Channel
import java.nio.file.Path
import java.util.*

/*context used in indexing with all necessary components*/
class TrigramIndexingContext(
    val folderPath: Path,
    val indexingState: TrigramIndexingState,
    val resultPathQueue: Queue<Path>,
    val trigramMap: TrigramMap
) {

    /*Unlimited capacity is used to achieve different independent speed of indexing parts: walking files, parsing files, saving triplets*/
    private val channelCapacity = Channel.UNLIMITED
    val visitedPathChannel = Channel<Path>(channelCapacity)
    val indexedPathChannel = Channel<Path>(channelCapacity)
    val tripletInPathChannel = Channel<Pair<String, Path>>(channelCapacity)

}