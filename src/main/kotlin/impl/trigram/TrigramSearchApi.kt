package impl.trigram

import api.*
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import api.tools.syncPerformIndex
import kotlinx.coroutines.*
import utils.WithLogging
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.isDirectory

/*Trigram implementation of Search Api without indexing and any optimizations.
  Can be used as etalon to check results, but not for performance and flexibility.
* */
class TrigramSearchApi : SearchApi, WithLogging() {
    //TODO fix structure class
    private val trigramMapByFolder: MutableMap<Path, TrigramMap> = mutableMapOf()
    private val indexer = TrigramIndexer()
    private val searcher = TrigramSearcher()

    /*Get index state. Using fo tests. It is not from interface*/
    fun getTrigramImmutableMap(folderPath: Path) =
        trigramMapByFolder[folderPath]?.cloneMap() ?: emptyMap()

    //FIXME GlobalScope
    /*Creates index at folder and saves in inner structure.
    * Works asynchronously.
    * */
    @OptIn(DelicateCoroutinesApi::class)
    override fun createIndexAtFolder(folderPath: Path): IndexingState {
        validatePath(folderPath) // TODO good question - should be thrown exception here or no?
        val completableFuture = CompletableFuture<List<Path>>()

        val indexingState = TrigramIndexingState(completableFuture)
        val deferred = GlobalScope.async {
            indexer.asyncIndexing(folderPath, completableFuture, indexingState, trigramMapByFolder)
        }
        fun cancelIndexing() {
            deferred.cancel(CancellationException())
        }
        indexingState.addCancelationAction(::cancelIndexing)
        return indexingState
    }

    /*Searches token in folder by using index in trigramMap, if there is no index, it performs it from the start.*/
    @OptIn(DelicateCoroutinesApi::class)
    override fun searchString(folderPath: Path, token: String, settings: SearchSettings): SearchingState {
        LOG.finest("started")
        validateToken(token)
        validatePath(folderPath)
        val completableFuture = CompletableFuture<List<TokenMatch>>()
        //FIXME - not asynchronous
        val trigramMap: TrigramMap = getTrigramMapOrCalculate(folderPath)

        val searchingState = TrigramSearchingState(completableFuture)
        val deferred = GlobalScope.async {
            searcher.asyncSearching(folderPath, token, trigramMap, completableFuture, searchingState)
        }
        fun cancelIndexing() {
            deferred.cancel(CancellationException())
        }
        searchingState.addCancelationAction(::cancelIndexing)
        return TrigramSearchingState(completableFuture)
    }

    /*Checks if there is index for folder in inner structure*/
    override fun hasIndexAtFolder(folderPath: Path): Boolean = trigramMapByFolder.contains(folderPath)

    /*Removes index at folder in inner structure*/
    override fun removeIndexAtFolder(folderPath: Path): Boolean = trigramMapByFolder.remove(folderPath) != null

    /*Removes full index by clearing inner structure*/
    override fun removeFullIndex() = trigramMapByFolder.clear()

    /*Takes folders with index from inner structure*/
    override fun getAllIndexedFolders(): List<Path> = trigramMapByFolder.keys.toList()

    /*Gets or recalculate trigram map*/
    private fun getTrigramMapOrCalculate(folderPath: Path): TrigramMap {
        val previouslyCalculatedTrigramMap = trigramMapByFolder[folderPath]
        if (previouslyCalculatedTrigramMap != null) {
            return previouslyCalculatedTrigramMap
        }
        syncPerformIndex(folderPath)
        return trigramMapByFolder[folderPath]!! //now it should exist
    }

    /*Validates token:
   * it cannot have less than 3 characters,
   * it cannot have symbols of changing line \n, \r.
   * */
    private fun validateToken(token: String) {
        if (token.length < 3) {
            throw IllegalArgumentSearchException("Token is too small, it has length less than 3 characters.")
        }
        for (forbiddenChar in forbiddenCharsInToken) {
            if (token.contains(forbiddenChar)) {
                throw IllegalArgumentSearchException("Token has forbidden character")
            }
        }
    }

    /*Validates path for folder:
    * it should be a folder.
    * */
    private fun validatePath(folderPath: Path) {
        if (!folderPath.isDirectory()) {
            throw NotDirSearchException(folderPath)
        }
    }

    companion object {
        /*Forbidden to use these characters in token.*/
        private val forbiddenCharsInToken: List<Char> = listOf('\n', '\r')

    }
}
