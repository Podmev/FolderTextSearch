package trigram

import api.*
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import api.tools.syncPerformIndex
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import utils.WithLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.stream.Stream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines
import kotlin.streams.asSequence

/*Trigram implementation of Search Api without indexing and any optimizations.
  Can be used as etalon to check results, but not for performance and flexibility.
* */
class TrigramSearchApi : SearchApi, WithLogging() {
    private val trigramMapByFolder: MutableMap<Path, TrigramMap> = mutableMapOf()

    /*Get index state. Using fo tests. It is not from interface*/
    fun getTrigramImmutableMap(folderPath: Path) =
        trigramMapByFolder[folderPath]?.cloneMap() ?: emptyMap()

    //FIX GlobalScope
    /*In this implementation index is empty, so even no files are added.*/
    @OptIn(DelicateCoroutinesApi::class)
    override fun createIndexAtFolder(folderPath: Path): IndexingState {
        validatePath(folderPath) // TODO good question - should be thrown exception here or no?
        val completableFuture = CompletableFuture<List<Path>>()
        val indexingState = TrigramIndexingState(completableFuture)
        val deferred = GlobalScope.async { asyncIndexing(folderPath, completableFuture, indexingState) }
        return indexingState
    }

    /*Main logic of indexing, checks if index is created already.
    * If there is no index, it starts indexing.
    * Has 4 processes:
    *  - walking files,
    *  - indexing files,
    *  - reading paths of already indexed files to save progress,
    *  - reading found triplets in each file to save in trigramMap,
    * In the end it fills result list of indexed files in CompletableFuture object
    * */
    private suspend fun asyncIndexing(
        folderPath: Path,
        future: CompletableFuture<List<Path>>,
        indexingState: TrigramIndexingState
    ) = coroutineScope {
        LOG.finest("started for folder: $folderPath")
        //TODO think about which structure is better choice for resultPathQueue: LinkedBlockingQueue or others
        val resultPathQueue: Queue<Path> = LinkedBlockingQueue()
        val foundTrigramMap: TrigramMap? = trigramMapByFolder[folderPath]
        coroutineScope {
            if (foundTrigramMap == null) {
                val trigramMap = TrigramMap()
                trigramMapByFolder[folderPath] = trigramMap
                LOG.finest("created new trigramMap for folder $folderPath")
                val indexingContext = TrigramIndexingContext(folderPath, indexingState, resultPathQueue, trigramMap)
                launch { asyncWalkingFiles(indexingContext) }
                launch { asyncIndexingFiles(indexingContext) }
                launch { asyncReadingIndexedPathChannel(indexingContext) }
                launch { asyncReadingTripletInPathChannel(indexingContext) }
            }
        }
        //here we wait all coroutines to finish
        val resultPathList = resultPathQueue.toList()
        future.complete(resultPathList)
        LOG.finest("finished for folder: $folderPath, indexed ${resultPathList.size} files")
    }

    /*Walks files: on each file - increment counter and send path in visited path channel.
    * In the end it closes visited path channel.
    * */
    private suspend fun asyncWalkingFiles(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started for folder: ${indexingContext.folderPath}")

        //TODO try to use stream
        val filePaths: List<Path> = withContext(Dispatchers.IO) {
            Files.walk(indexingContext.folderPath)
        }.use { it: Stream<Path> ->
            it.asSequence()
                .filter { path -> path.isRegularFile() }
                //.map { path -> path.apply { println(path) } }
                .toList()
        }
        val pathsNumber = filePaths.size
        LOG.finest("created filePaths: $pathsNumber")
        val totalFilesNumber = pathsNumber.toLong()
        val setupSuccessful = indexingContext.indexingState.setTotalFilesNumber(totalFilesNumber)
        LOG.finest("setup totalFilesNumber (successfully: $setupSuccessful) in indexing state: $totalFilesNumber")

        filePaths.asFlow().onEach { path ->
            LOG.finest("visiting file by path $path")
            indexingContext.visitedPathChannel.send(path)
            val visitedFilesNumber = indexingContext.visitedFilesNumber.incrementAndGet()
            LOG.finest("successfully visited $visitedFilesNumber")
        }.collect { }

        indexingContext.visitedPathChannel.close()
        LOG.finest("closed visitedPathChannel")

        LOG.finest("finished for folder: ${indexingContext.folderPath}")
    }

    /*On each visited path from visited path channel.
    * It constructs index for file and sends file path to indexed path channel.
    * In the end it closes 2 channels: indexed path channel and triplet in path channel.
    * */
    private suspend fun asyncIndexingFiles(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started for folder: ${indexingContext.folderPath}")

        for (path in indexingContext.visitedPathChannel) {
            constructIndexForFile(path, indexingContext)
            indexingContext.indexedPathChannel.trySendBlocking(path)
                .onSuccess { LOG.finest("send successfully path $path to indexedPathChannel") }
                .onFailure { t: Throwable? -> LOG.severe("Cannot send path in indexedPathChannel: ${t?.message}") }
            val indexedFilesNumber = indexingContext.indexedFilesNumber.incrementAndGet()
            LOG.finest("successfully visited $indexedFilesNumber")
        }
        indexingContext.indexedPathChannel.close()
        LOG.finest("closed indexedPathChannel")
        indexingContext.tripletInPathChannel.close()
        LOG.finest("closed tripletInPathChannel")
        LOG.finest("finished for folder: ${indexingContext.folderPath}")
    }

    /*Lazy for each line separately constructs index.
    * */
    private suspend fun constructIndexForFile(path: Path, indexingContext: TrigramIndexingContext) {
        LOG.finest("started for path: $path")
        //TODO maybe add flow too
        path.useLines { lines ->
            lines
                .forEachIndexed { lineIndex, line ->
                    constructIndexForLine(
                        path = path,
                        line = line,
                        lineIndex = lineIndex,
                        indexingContext = indexingContext
                    )
                }
        }
        LOG.finest("finished for path: $path")
    }

    /*Runs with sliding window of 3 characters and sends to triplet in path channel.*/
    private suspend fun constructIndexForLine(
        path: Path,
        line: String,
        lineIndex: Int,
        indexingContext: TrigramIndexingContext
    ) {
        LOG.finest("started line $lineIndex for path: $path")
        if (line.length < 3) return
        for (column in 0 until line.length - 2) {
            val triplet = line.substring(column, column + 3)
            LOG.finest("saving triplet $triplet at line $lineIndex, column: $column for path: $path")
            indexingContext.tripletInPathChannel.send(Pair(triplet, path))
        }
        LOG.finest("finished line $lineIndex for path: $path")
    }

    /*Reads indexed files, updates state and resultList.*/
    private suspend fun asyncReadingIndexedPathChannel(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started")
        for (path in indexingContext.indexedPathChannel) {
            LOG.finest("received indexed path and saving to state: $path")
            indexingContext.indexingState.addPathToBuffer(path)
            indexingContext.resultPathQueue.add(path)
        }
        LOG.finest("finished")
    }

    /*Reads triplets of chars by path in channel, saves in trigramMap and updates state.*/
    private suspend fun asyncReadingTripletInPathChannel(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started")
        for ((triplet, path) in indexingContext.tripletInPathChannel) {
            LOG.finest("received path for triplet: $triplet $path")
            indexingContext.trigramMap.addCharTripletByPath(triplet, path)
        }
        LOG.finest("finished")
    }

    /*Searches token in folder by using index in trigramMap, if there is no index, it performs it from the start.*/
    override fun searchString(folderPath: Path, token: String, settings: SearchSettings): SearchingState {
        LOG.finest("started")
        validateToken(token)
        validatePath(folderPath)
        val completableFuture = CompletableFuture<List<TokenMatch>>()
        //TODO put in async code search
        val trigramMap: TrigramMap = getTrigramMapOrCalculate(folderPath)
        val paths = getPathsByToken(trigramMap, token)
        LOG.finest("got ${paths.size} paths for token $token by trigramMap in folder $folderPath: $paths")
        val tokenMatches = searchStringInPaths(paths, token)
        LOG.finest("got ${tokenMatches.size} token matches for token $token in folder $folderPath: $paths")
        completableFuture.complete(tokenMatches)
        return TrigramSearchingState(completableFuture)
    }

    /*Gets or recalculate trigram map*/
    private fun getTrigramMapOrCalculate(folderPath: Path): TrigramMap {
        val previouslyCalculatedTrigramMap = trigramMapByFolder[folderPath]
        if (previouslyCalculatedTrigramMap != null) {
            return previouslyCalculatedTrigramMap
        }
        syncPerformIndex(folderPath)
        return trigramMapByFolder[folderPath]!! //now it should exist
    }

    /*Find all file paths, which contains all sequence char triplets from token.
    * */
    private fun getPathsByToken(trigramMap: TrigramMap, token: String): Set<Path> {
        if (token.length < 3) return emptySet()
        return (0 until token.length - 2)
            .map { column -> token.substring(column, column + 3) }
            .map { triplet -> trigramMap.getPathsByCharTriplet(triplet) }
            .reduce { pathSet1: Set<Path>, pathSet2: Set<Path> -> pathSet1.intersect(pathSet2) }
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

    /*Searches token in paths.
    * We already know that each of them has every triple sequential characters.
    * */
    private fun searchStringInPaths(paths: Collection<Path>, token: String): List<TokenMatch> =
        paths.asSequence()
            .filter { path -> path.isRegularFile() }
            .flatMap { file -> searchStringInFile(file, token) }
            .toList()

    //TODO think if it is possible to make index for line number
    /*Searches token by single path.
    * Searches for every line separately.
    * */
    private fun searchStringInFile(filePath: Path, token: String): List<TokenMatch> =
        filePath.useLines { lines ->
            lines
                .flatMapIndexed { lineIndex, line -> searchStringInLine(filePath, line, token, lineIndex) }
                .toList()
        }

    /*Searches token by single line and creates list of tokenMatches.
   * */
    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): List<TokenMatch> {
        LOG.finest("#$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(filePath, lineIndex.toLong(), it.toLong()) }.toList()
    }

    companion object {
        /*Forbidden to use these characters in token.*/
        private val forbiddenCharsInToken: List<Char> = listOf('\n', '\r')
    }
}

/*Finds all indices of starts of token in line.*/
fun String.indicesOf(token: String, ignoreCase: Boolean = false): Sequence<Int> {
    fun next(startOffset: Int) = this.indexOf(token, startOffset, ignoreCase).takeIf { it != -1 }
    return generateSequence(next(0)) { prevIndex -> next(prevIndex + 1) }
}