package impl.trigram

import api.*
import api.exception.BusySearchException
import api.exception.IllegalArgumentSearchException
import api.exception.NoIndexSearchException
import api.exception.NotDirSearchException
import impl.trigram.dirwatcher.FolderWatchProcessor
import impl.trigram.dirwatcher.WatcherHolder
import impl.trigram.incremental.TrigramIncrementalIndexer
import impl.trigram.map.SimpleTrigramMap
import impl.trigram.map.TimedTrigramMap
import impl.trigram.map.TrigramMap
import impl.trigram.map.TrigramMapType
import kotlinx.coroutines.*
import utils.WithLogging
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.isDirectory

/**
 * Trigram implementation of Search Api without indexing and any optimizations.
 * Can be used as etalon to check results, but not for performance and flexibility.
 * */
class TrigramSearchApi(trigramMapType: TrigramMapType = TrigramMapType.TIMED) : SearchApi, WithLogging() {
    /**
     * Flag for incremental indexing activity
     * True - it is going indexing, false - no
     * */
    private val incrementalIndexingInProcess = AtomicBoolean(false)

    /**
     * Flag for indexing activity
     * True - it is going some indexing, false - no
     * */
    private val indexInProcess = AtomicBoolean(false)

    private val trigramMapByFolder: MutableMap<Path, TrigramMap> = mutableMapOf()
    private val watcherHolder = WatcherHolder()
    private val indexer = TrigramIndexer(
        { getTrigramMapCreatorByType(trigramMapType) },
        { path: Path -> if (incrementalIndexingInProcess.get()) watcherHolder.addWatch(path) }
    )
    private val searcher = TrigramSearcher()
    private val incrementalIndexer: TrigramIncrementalIndexer = TrigramIncrementalIndexer(trigramMapByFolder)
    private val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

    /**
     * job for permanent incremental indexing
     * */
    private var incrementalIndexingJob: Job? = null

    /**
     * Get index state. Using fo tests. It is not from interface
     * */
    fun getTrigramImmutableMap(folderPath: Path): Map<String, Set<Path>> =
        trigramMapByFolder[folderPath]?.clonePathsByTripletsMap() ?: emptyMap()

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
        folderPath: Path, token: String, settings: SearchSettings
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

    @OptIn(DelicateCoroutinesApi::class)
    override fun startIncrementalIndexing(withInitialUpdate: Boolean): Boolean {
        if (!incrementalIndexingInProcess.compareAndSet(false, true)) {
            return false
        }

        val job = GlobalScope.launch {
            LOG.finest("started incremental indexing")
            LOG.finest("setting up watcherHolder")
            watcherHolder.setup()
            //creating watch for all paths
            LOG.finest("adding all existed indexed folders to watcher holder")
            trigramMapByFolder.keys.forEach { watcherHolder.addWatch(it) }
            if (withInitialUpdate) {
                //updating indices for all folder at start
                coroutineScope {
                    launch { incrementalIndexer.updateAllIndicesOnIncrementalIndexingStart() }
                }
            }

            //subscribing events from fil system and send to incrementalIndexer
            LOG.finest("running asyncProcessEvents in folderWatchProcessor")
            launch { folderWatchProcessor.asyncProcessEvents(incrementalIndexer) }
            //read events and apply to index (trigramMapByFolder)
            LOG.finest("running asyncProcessFileChanges in incrementalIndexer")
            launch { incrementalIndexer.asyncProcessFileChanges() }
        }
        job.invokeOnCompletion {
            LOG.finest("finished incremental indexing")
        }
        incrementalIndexingJob = job
        return true
    }

    override fun stopIncrementalIndexing(): Boolean {
        if (!incrementalIndexingInProcess.compareAndSet(true, false)) {
            return false
        }
        runBlocking {
            //removing all watches when incremental indexing finishes
            LOG.finest("cleaning up watcherHolder")
            watcherHolder.cleanUp()
            LOG.finest("sending cancel to incrementalIndexingJob")
            incrementalIndexingJob?.cancel(CancellationException("Stopped incremental indexing"))
            incrementalIndexingJob?.join()
            LOG.finest("set link to incrementalIndexingJob as null")
            incrementalIndexingJob = null
            LOG.finest("incremental indexing is stopped")
        }
        return true
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
        watcherHolder.removeWatch(folderPath)
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
        watcherHolder.removeAllWatches()
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

        /**
         * Chooses type of trigramMap creator
         * */
        private fun getTrigramMapCreatorByType(trigramMapType: TrigramMapType): TrigramMap =
            when (trigramMapType) {
                TrigramMapType.SIMPLE -> SimpleTrigramMap()
                TrigramMapType.TIMED -> TimedTrigramMap()
            }

    }
}
