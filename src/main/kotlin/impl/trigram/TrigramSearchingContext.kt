package impl.trigram

import api.TokenMatch
import kotlinx.coroutines.channels.Channel
import java.nio.file.Path
import java.util.*

/*context used in searching with all necessary components*/
class TrigramSearchingContext(
    val folderPath: Path,
    val token: String,
    val searchingState: TrigramSearchingState,
    val resultTokenMatchQueue: Queue<TokenMatch>,
    val trigramMap: TrigramMap
) {

    /*Unlimited capacity is used to achieve different independent speed of indexing parts: walking files, parsing files, saving triplets*/
    private val channelCapacity = Channel.UNLIMITED
    val narrowedPathChannel = Channel<Path>(channelCapacity)
    val fileLineChannel = Channel<LineInFile>(channelCapacity)
    //TODO add lineBatchChannel
    val tokenMatchChannel = Channel<TokenMatch>(channelCapacity)

}