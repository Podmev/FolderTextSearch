package impl.trigram

import api.*
import api.exception.BusySearchException
import api.exception.IllegalArgumentSearchException
import api.exception.NoIndexSearchException
import api.exception.NotDirSearchException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import utils.WithLogging
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.isDirectory

/**
 * Trigram implementation of Search Api without indexing and any optimizations.
 * Can be used as etalon to check results, but not for performance and flexibility.
 * */
class TrigramSearchApi : SearchApi, WithLogging() {
    private val trigramMapByFolder: MutableMap<Path, TrigramMap> = mutableMapOf()
    private val indexer = TrigramIndexer()
    private val searcher = TrigramSearcher()

    /**
     * Flag for indexing activity
     * True - it is going some indexing, false - no
     * */
    private val indexInProcess = AtomicBoolean(false)

    /**
     * Get index state. Using fo tests. It is not from interface
     * */
    fun getTrigramImmutableMap(folderPath: Path) =
        trigramMapByFolder[folderPath]?.cloneMap() ?: emptyMap()

    /**
     * Creates index at folder and saves in inner structure.
     * Works asynchronously.
     * */
    @OptIn(DelicateCoroutinesApi::class)
    override fun createIndexAtFolder(folderPath: Path): IndexingState {
        validatePath(folderPath)
        if (!indexInProcess.compareAndSet(false, true)) {
            throw BusySearchException("Cannot create index while indexing")
        }
        val completableFuture = CompletableFuture<List<Path>>()

        val indexingState = TrigramIndexingState(completableFuture)
        val deferred = GlobalScope.async {
            indexer.asyncIndexing(folderPath, completableFuture, indexingState, trigramMapByFolder, indexInProcess)
        }

        fun cancelIndexing() {
            indexingState.changeStatus(ProgressableStatus.CANCELLING)
            deferred.cancel(CancellationException())
            LOG.finest("deferred.cancel()")
        }
        indexingState.addCancellationAction(::cancelIndexing)
        return indexingState
    }

    /**
     * Searches token in folder by using index in trigramMap.
     * If there is no index, it throws exception.
     * If some index is calculation at the moment, it throws exception.
     * */
    @OptIn(DelicateCoroutinesApi::class)
    override fun searchString(folderPath: Path, token: String, settings: SearchSettings): SearchingState {
        LOG.finest("started")
        if (indexInProcess.get()) {
            throw BusySearchException("Cannot search while indexing")
        }
        validateToken(token)
        validatePath(folderPath)
        val completableFuture = CompletableFuture<List<TokenMatch>>()
        val trigramMap: TrigramMap = getTrigramMapOrThrowException(folderPath)

        val searchingState = TrigramSearchingState(completableFuture)
        val deferred = GlobalScope.async {
            searcher.asyncSearching(folderPath, token, trigramMap, completableFuture, searchingState)
        }

        fun cancelSearching() {
            searchingState.changeStatus(ProgressableStatus.CANCELLING)
            deferred.cancel(CancellationException())
            LOG.finest("deferred.cancel()")
        }
        searchingState.addCancellationAction(::cancelSearching)
        return searchingState
    }

    /**
     * Operation with indexing and searching together
     * */
    @OptIn(DelicateCoroutinesApi::class)
    override fun indexAndSearchString(
        folderPath: Path,
        token: String,
        settings: SearchSettings
    ): IndexingAndSearchingState {
        validatePath(folderPath)
        validateToken(token)
        if (!indexInProcess.compareAndSet(false, true)) {
            throw BusySearchException("Cannot create index while indexing")
        }
        val completableIndexFuture = CompletableFuture<List<Path>>()
        val completableSearchFuture = CompletableFuture<List<TokenMatch>>()

        val indexingState = TrigramIndexingState(completableIndexFuture)
        val searchingState = TrigramSearchingState(completableSearchFuture)
        val indexingAndSearchingState = TrigramIndexingAndSearchingState(indexingState, searchingState)

        val deferred = GlobalScope.async {
            val trigramMapDeferred = async {
                indexer.asyncIndexing(
                    folderPath = folderPath,
                    future = completableIndexFuture,
                    indexingState = indexingState,
                    trigramMapByFolder = trigramMapByFolder,
                    indexInProcess = indexInProcess
                )
            }
            val trigramMap: TrigramMap = trigramMapDeferred.await()
            searcher.asyncSearching(folderPath, token, trigramMap, completableSearchFuture, searchingState)
        }

        fun cancelIndexing() = indexingState.changeStatus(ProgressableStatus.CANCELLING)
        fun cancelSearching() = searchingState.changeStatus(ProgressableStatus.CANCELLING)
        fun cancelIndexingAndSearching() = deferred.cancel(CancellationException())
        indexingState.addCancellationAction(::cancelIndexing)
        searchingState.addCancellationAction(::cancelSearching)
        indexingAndSearchingState.addCancellationAction(::cancelIndexingAndSearching)
        return indexingAndSearchingState
    }

    /**
     * Checks if there is index for folder in inner structure
     * */
    override fun hasIndexAtFolder(folderPath: Path): Boolean = trigramMapByFolder.contains(folderPath)

    /**
     * Removes index at folder in inner structure
     * */
    override fun removeIndexAtFolder(folderPath: Path): Boolean {
        if (!indexInProcess.compareAndSet(false, true)) {
            throw BusySearchException("Cannot remove full index while indexing")
        }
        val result = trigramMapByFolder.remove(folderPath) != null
        indexInProcess.set(false)
        return result
    }

    /**
     * Removes full index by clearing inner structure
     * */
    override fun removeFullIndex() {
        if (!indexInProcess.compareAndSet(false, true)) {
            throw BusySearchException("Cannot remove full index while indexing")
        }
        trigramMapByFolder.clear()
        indexInProcess.set(false)
    }

    /**
     * Takes folders with index from inner structure
     * */
    override fun getAllIndexedFolders(): List<Path> = trigramMapByFolder.keys.toList()

    /**
     * Gets ready trigram map of throws exception NoIndexSearchException
     * */
    private fun getTrigramMapOrThrowException(folderPath: Path): TrigramMap {
        val previouslyCalculatedTrigramMap = trigramMapByFolder[folderPath]
        if (previouslyCalculatedTrigramMap != null) {
            return previouslyCalculatedTrigramMap
        }
        throw NoIndexSearchException("Cannot search without prepared index, run createIndexAtFolder at first")
    }

    /**
     * Validates token:
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

    /**
     * Validates path for folder:
     * it should be a folder.
     * */
    private fun validatePath(folderPath: Path) {
        if (!folderPath.isDirectory()) {
            throw NotDirSearchException(folderPath)
        }
    }

    companion object {
        /**
         * Forbidden to use these characters in token.
         * */
        private val forbiddenCharsInToken: List<Char> = listOf('\n', '\r')

    }
}
