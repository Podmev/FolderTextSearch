package trigram

import api.IndexingState
import java.nio.file.Path
import java.util.concurrent.Future

//TODO implement correct
/*State is complete at the start, so no indexing
* */
class TrigramIndexingState(override val result: Future<List<Path>>) : IndexingState {

    override val finished: Boolean = true
    override val progress: Double = 1.0
    override fun cancel() {}
    override fun getBufferPartResult(flush: Boolean): List<Path> {
        //TODO("Not yet implemented")
        return emptyList()
    }

    fun addPathToBuffer(path: Path){
        //TODO("Not yet implemented")
    }
}