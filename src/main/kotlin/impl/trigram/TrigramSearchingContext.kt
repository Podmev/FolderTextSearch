package impl.trigram

import api.TokenMatch
import kotlinx.coroutines.channels.Channel
import java.nio.file.Path
import java.util.*

/*context used in searching with all necessary components*/
internal class TrigramSearchingContext(
    /**
     * Folder path for searching.
     */
    val folderPath: Path,
    /**
     * Token to search.
     */
    val token: String,
    /**
     * Searching state, contains mutable state.
     */
    val searchingState: TrigramSearchingState,
    /**
     * Queue of found token matches, used for result.
     */
    val resultTokenMatchQueue: Queue<TokenMatch>,
    /**
     * Internal structure for index.
     */
    val trigramMap: TrigramMap
) {

    /*Unlimited capacity is used to achieve different independent speed of searching parts:
    * walking files, parsing file lines, saving token matches
    * */
    private val channelCapacity = Channel.UNLIMITED
    val narrowedPathChannel = Channel<Path>(channelCapacity)
    val fileLineChannel = Channel<LineInFile>(channelCapacity)
    //TODO add lineBatchChannel
    val tokenMatchChannel = Channel<TokenMatch>(channelCapacity)

}