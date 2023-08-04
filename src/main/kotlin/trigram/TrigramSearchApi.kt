package trigram

import api.*
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import api.tools.syncPerformIndex
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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

//TODO implement correct
/*Trigram implementation of Search Api without indexing and any optimizations
  Can be used as etalon to check results, but not for performance and flexibility
* */
class TrigramSearchApi : SearchApi, WithLogging() {
    private val trigramMapByFolder: MutableMap<Path, TrigramMap> = mutableMapOf()

    /*Get index state. Using fo tests*/
    fun getTrigramImmutableMap(folderPath: Path) =
        trigramMapByFolder[folderPath]?.cloneMap() ?: emptyMap()

    /*in this implementation index is empty, so even no files are added*/
    @OptIn(DelicateCoroutinesApi::class) //FIX GlobalScope
    override fun createIndexAtFolder(folderPath: Path): IndexingState {
        validatePath(folderPath) // TODO good question - should be thrown exception here or no?
        val completableFuture = CompletableFuture<List<Path>>()
        val indexingState = TrigramIndexingState(completableFuture)
        val deferred = GlobalScope.async { asyncIndexing(folderPath, completableFuture, indexingState) }
        return indexingState
    }

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
                val indexedPathChannel = Channel<Path>()
                val tripletInPathChannel = Channel<Pair<String, Path>>()
                launch { asyncWalkingFiles(folderPath, indexedPathChannel, tripletInPathChannel) }
                launch { asyncReadingIndexedPathChannel(indexedPathChannel, resultPathQueue, indexingState) }
                launch { asyncReadingTripletInPathChannel(tripletInPathChannel, trigramMap) }
            }
        }
        //here we wait all coroutines to finish
        val resultPathList = resultPathQueue.toList()
        future.complete(resultPathList)
        LOG.info("finished for folder: $folderPath, indexed ${resultPathList.size} files")
    }

    private suspend fun asyncWalkingFiles(
        folderPath: Path,
        indexedPathChannel: Channel<Path>,
        tripletInPathChannel: Channel<Pair<String, Path>>
    ) = coroutineScope {
        LOG.finest("started for folder: $folderPath")

        //TODO try to use stream
        val filePaths: List<Path> = withContext(Dispatchers.IO) {
            Files.walk(folderPath)
        }.use { it: Stream<Path> ->
            it.asSequence()
                .filter { path -> path.isRegularFile() }
                //.map { path -> path.apply { println(path) } }
                .toList()
        }
        LOG.finest("created filePaths: ${filePaths.size}")

        filePaths.asFlow().onEach { path ->
            LOG.finest("visiting file by path $path")
            constructIndexForFile(path, tripletInPathChannel)
            indexedPathChannel.trySendBlocking(path)
                .onSuccess { LOG.finest("send successfully path $path to indexedPathChannel") }
                .onFailure { t: Throwable? -> LOG.severe("Cannot send path in indexedPathChannel: ${t?.message}") }
        }
            .collect { }
        indexedPathChannel.close()
        LOG.finest("closed indexedPathChannel")
        tripletInPathChannel.close()
        LOG.finest("closed tripletInPathChannel")
        LOG.finest("finished for folder: $folderPath")
    }

    private suspend fun constructIndexForFile(path: Path, tripletInPathChannel: Channel<Pair<String, Path>>) {
        LOG.finest("started for path: $path")
        //TODO maybe add flow too
        path.useLines { lines ->
            lines
                .forEachIndexed { lineIndex, line ->
                    constructIndexForLine(
                        path = path,
                        line = line,
                        lineIndex = lineIndex,
                        tripletInPathChannel = tripletInPathChannel
                    )
                }
        }
        LOG.finest("finished for path: $path")
    }

    private suspend fun constructIndexForLine(
        path: Path,
        line: String,
        lineIndex: Int,
        tripletInPathChannel: Channel<Pair<String, Path>>
    ) {
        LOG.finest("started line $lineIndex for path: $path")
        if (line.length < 3) return
        for (column in 0 until line.length - 2) {
            val triplet = line.substring(column, column + 3)
            LOG.finest("saving triplet $triplet at line $lineIndex, column: $column for path: $path")
            tripletInPathChannel.send(Pair(triplet, path))
        }
        LOG.finest("finished line $lineIndex for path: $path")
    }

    private suspend fun asyncReadingIndexedPathChannel(
        indexedPathChannel: Channel<Path>,
        resultPathQueue: Queue<Path>,
        indexingState: TrigramIndexingState
    ) = coroutineScope {
        LOG.finest("started")
        for (path in indexedPathChannel) {
            LOG.finest("received indexed path and saving to state: $path")
            indexingState.addPathToBuffer(path)
            resultPathQueue.add(path)
        }
        LOG.finest("finished")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun asyncReadingTripletInPathChannel(
        tripletInPathChannel: Channel<Pair<String, Path>>,
        trigramMap: TrigramMap,
    ) = coroutineScope {
        LOG.finest("started")
        for ((triplet, path) in tripletInPathChannel) {
            LOG.finest("received path for triplet: $triplet $path")
            trigramMap.addCharTripletByPath(triplet, path)
        }
        LOG.finest("finished")
    }


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

    private fun getTrigramMapOrCalculate(folderPath: Path): TrigramMap {
        val previouslyCalculatedTrigramMap = trigramMapByFolder[folderPath]
        if (previouslyCalculatedTrigramMap != null) {
            return previouslyCalculatedTrigramMap
        }
        syncPerformIndex(folderPath)
        return trigramMapByFolder[folderPath]!! //now it should exist
    }

    /*Find all file paths, which contains all sequence char triplets from token
    * */
    private fun getPathsByToken(trigramMap: TrigramMap, token: String): Set<Path> {
        if (token.length < 3) return emptySet()
        return (0 until token.length - 2)
            .map { column -> token.substring(column, column + 3) }
            .map { triplet -> trigramMap.getPathsByCharTriplet(triplet) }
            .reduce { pathSet1: Set<Path>, pathSet2: Set<Path> -> pathSet1.intersect(pathSet2) }
    }

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

    private fun validatePath(folderPath: Path) {
        if (!folderPath.isDirectory()) {
            throw NotDirSearchException(folderPath)
        }
    }

    private fun searchStringInPaths(paths: Collection<Path>, token: String): List<TokenMatch> =
        paths.asSequence()
            .filter { path -> path.isRegularFile() }
            .flatMap { file -> searchStringInFile(file, token) }
            .toList()

    private fun searchStringInFile(filePath: Path, token: String): List<TokenMatch> =
        filePath.useLines { lines ->
            lines
                .flatMapIndexed { lineIndex, line -> searchStringInLine(filePath, line, token, lineIndex) }
                .toList()
        }

    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): List<TokenMatch> {
        LOG.finest("#$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(filePath, lineIndex.toLong(), it.toLong()) }.toList()
    }

    companion object {
        private val forbiddenCharsInToken: List<Char> = listOf('\n', '\r')
    }
}

fun String.indicesOf(token: String, ignoreCase: Boolean = false): Sequence<Int> {
    fun next(startOffset: Int) = this.indexOf(token, startOffset, ignoreCase).takeIf { it != -1 }
    return generateSequence(next(0)) { prevIndex -> next(prevIndex + 1) }
}