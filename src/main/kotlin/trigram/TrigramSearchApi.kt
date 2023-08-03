package trigram

import api.*
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import utils.WithLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
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
        LOG.info("started for folder: $folderPath")
        val resultPathList = ArrayList<Path>()
        val foundTrigramMap: TrigramMap? = trigramMapByFolder[folderPath]
        coroutineScope {
            if (foundTrigramMap == null) {
                val trigramMap = TrigramMap()
                trigramMapByFolder[folderPath] = trigramMap
                LOG.info("created new trigramMap for folder $folderPath")
                val indexedPathChannel = Channel<Path>(UNLIMITED)
                val tripletInPathChannel = Channel<Pair<String, Path>>()
                launch { asyncWalkingFiles(folderPath, indexedPathChannel, tripletInPathChannel) }
                launch { asyncReadingIndexedPathChannel(indexedPathChannel, resultPathList, indexingState) }
                launch { asyncReadingTripletInPathChannel(tripletInPathChannel, trigramMap) }
            }
        }
        //here we wait all coroutines to finish
        future.complete(resultPathList)
        LOG.info("finished for folder: $folderPath")
    }

    private suspend fun asyncWalkingFiles(
        folderPath: Path,
        indexedPathChannel: Channel<Path>,
        tripletInPathChannel: Channel<Pair<String, Path>>
    ) = coroutineScope {
        LOG.info("started for folder: $folderPath")

        //TODO try to use stream
        val filePaths: List<Path> = withContext(Dispatchers.IO) {
            Files.walk(folderPath)
        }.use { it: Stream<Path> ->
            it.asSequence()
                .filter { path -> path.isRegularFile() }
                .map { path -> path.apply { println(path) } }
                .toList()
        }
        LOG.info("created filePaths: ${filePaths.size}")

//        coroutineScope {
        filePaths.asFlow().onEach { path ->
            LOG.info("visiting file by path $path")
            constructIndexForFile(path, tripletInPathChannel)
            //indexedPathChannel.send(path)
        }
            //.launchIn(this)
        .collect{ println(it) }
//            .collectLatest{
//                println(it)
                indexedPathChannel.close()
                LOG.info("closed indexedPathChannel")
                tripletInPathChannel.close()
                LOG.info("closed tripletInPathChannel")
//            }
//        }
//        LOG.info("finished iterating paths and indexing files")
//        indexedPathChannel.close()
//        LOG.info("closed indexedPathChannel")
//        tripletInPathChannel.close()
//        LOG.info("closed tripletInPathChannel")
//        flow<Path> {
//            for (path in filePaths) {
//                //TODO Decide where should be logic here or in "collect" down here
//                constructIndexForFile(path, tripletInPathChannel)
//                indexedPathChannel.send(path)
//                emit(path)
//            }
//        }
//        LOG.info("created pathFlow")
//        pathFlow.collectFlowAsState()
////        { path ->
////            LOG.info("visiting file by path $path")
////        }
        LOG.info("finished for folder: $folderPath")
    }

    private suspend fun constructIndexForFile(path: Path, tripletInPathChannel: Channel<Pair<String, Path>>) {
        LOG.info("started for path: $path")
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
        LOG.info("finished for path: $path")
    }

    private suspend fun constructIndexForLine(
        path: Path,
        line: String,
        lineIndex: Int,
        tripletInPathChannel: Channel<Pair<String, Path>>
    ) {
        LOG.info("started line $lineIndex for path: $path")
        if (line.length < 3) return
        for (column in 0 until line.length - 2) {
            val triplet = line.substring(column, column + 3)
            LOG.info("saving triplet $triplet at line $lineIndex, column: $column for path: $path")
            tripletInPathChannel.send(Pair(triplet, path))
        }
        LOG.info("finished line $lineIndex for path: $path")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun asyncReadingIndexedPathChannel(
        indexedPathChannel: Channel<Path>,
        resultPathList: MutableList<Path>,
        indexingState: TrigramIndexingState
    ) = coroutineScope {
        LOG.info("started")
        for (path in indexedPathChannel){
            LOG.info("received indexed path and saving to state: $path")
            indexingState.addPathToBuffer(path)
            resultPathList.add(path)
        }
        LOG.info("finished")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun asyncReadingTripletInPathChannel(
        tripletInPathChannel: Channel<Pair<String, Path>>,
        trigramMap: TrigramMap,
    ) = coroutineScope {
        LOG.info("started")
        for((triplet, path) in tripletInPathChannel){
            LOG.info("received path for triplet: $triplet $path")
            trigramMap.addCharTripletByPath(triplet, path)
        }
        LOG.info("finished")
    }


    override fun searchString(folderPath: Path, token: String, settings: SearchSettings): SearchingState {
        validateToken(token)
        validatePath(folderPath)
        val completableFuture = CompletableFuture<List<TokenMatch>>()
        //TODO put in async code search
        val tokenMatches = searchStringInFolder(folderPath, token)
        completableFuture.complete(tokenMatches)
        return TrigramSearchingState(completableFuture)
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

    private fun searchStringInFolder(folderPath: Path, token: String): List<TokenMatch> =
        Files.walk(folderPath).use {
            it.asSequence()
                .filter { path -> path.isRegularFile() }
                .flatMap { file -> searchStringInFile(file, token) }
                .toList()
        }

    private fun searchStringInFile(filePath: Path, token: String): List<TokenMatch> =
        filePath.useLines { lines ->
            lines
                .flatMapIndexed { lineIndex, line -> searchStringInLine(filePath, line, token, lineIndex) }
                .toList()
        }

    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): List<TokenMatch> {
        LOG.info("#$lineIndex, \"$line\", token: $token")
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