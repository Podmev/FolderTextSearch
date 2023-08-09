package impl.trigram

import api.TokenMatch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import utils.WithLogging
import utils.indicesOf
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.useLines

/**
 * Only logic of searching token using constructed index for TrigramSearApi
 */
class TrigramSearcher : WithLogging() {

    /**
     * Searches asynchronously for token in folder with using constructed index trigramMap
     */
    suspend fun asyncSearching(
        folderPath: Path,
        token: String,
        trigramMap: TrigramMap,
        future: CompletableFuture<List<TokenMatch>>,
        searchingState: TrigramSearchingState,
    ) = coroutineScope {
        try {
            LOG.finest("started for folder: $folderPath and token: \"$token\"")
            val resultTokenMatchQueue: Queue<TokenMatch> = LinkedBlockingQueue()
            coroutineScope {
                val searchingContext = TrigramSearchingContext(
                    folderPath = folderPath,
                    token = token,
                    searchingState = searchingState,
                    resultTokenMatchQueue = resultTokenMatchQueue,
                    trigramMap = trigramMap
                )
                coroutineScope {
                    launch { asyncWalkTokenAndNarrowPaths(searchingContext) }
                    launch { asyncSearchingInPaths(searchingContext) }
                    launch { asyncSearchingInFileLines(searchingContext) }
                    launch { asyncReadingTokenMatchesChannel(searchingContext) }
                    //TODO add for counting progress something similar
                }
            }
            //here we wait all coroutines to finish
            val resultPathList = resultTokenMatchQueue.toList()
            future.complete(resultPathList)
            LOG.finest("finished for folder: $folderPath and token: \"$token\", ${resultPathList.size} token matches")
        } catch (ex: CancellationException) {
            //TODO check if it is correct behaviour
            future.complete(emptyList())
            throw ex // Must let the CancellationException propagate
        } catch (th: Throwable) {
            //TODO check if it is correct behaviour
            future.complete(emptyList())
            LOG.severe("exception during making index: ${th.message}")
            th.printStackTrace()
        }
    }

    /*
    * Takes narrowed paths for token - only those files which has ALL together triplets of sequencial character from token
    * For each path it sends in channel narrowedPathChannel
    * */
    private suspend fun asyncWalkTokenAndNarrowPaths(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"${searchingContext.token}\"")
        //TODO make async way getPathsByToken
        val narrowedPaths = getPathsByToken(searchingContext.trigramMap, searchingContext.token)
        LOG.finest("got ${narrowedPaths.size} narrowed paths from trigramMap by token \"${searchingContext.token}\"")

        //TODO remove temporary block --start--
        //commands should be in flow and set total after, so we can cancel easily
        //can be extra flow for it
        narrowedPaths.forEach { searchingContext.searchingState.addVisitedPath(it) }
        searchingContext.searchingState.setTotalFilesByteSize()
        searchingContext.searchingState.setTotalFilesNumber()
        //TODO remove temporary block --end--

        narrowedPaths.asSequence().asFlow().onEach { path ->
                searchingContext.narrowedPathChannel.send(path)
                LOG.finest("sent path to channel narrowedPathChannel: $path")
                //searchingContext.searchingState.addVisitedPath(path)
            }.collect {}

        searchingContext.narrowedPathChannel.close()
        LOG.finest("closed channel narrowedPathChannel")

//        searchingContext.searchingState.setTotalFilesByteSize()
//        searchingContext.searchingState.setTotalFilesNumber()

        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"${searchingContext.token}\"")
    }

    /*For each path in narrowedPathChannel it sends all lines of file in channel fileLineChannel */
    private suspend fun asyncSearchingInPaths(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
        for (path in searchingContext.narrowedPathChannel) {
            LOG.finest("received path: $path")
            path.useLines { lines ->
                lines.withIndex().asFlow().onEach { (lineIndex, line) ->
                    searchingContext.fileLineChannel.send(LineInFile(path, lineIndex, line))
                }.collect {}
            }
        }
        searchingContext.fileLineChannel.close()
        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
    }

    /*For each line in fileLineChannel it sends all found token matches to tokenMatchChannel*/
    private suspend fun asyncSearchingInFileLines(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
        for (lineInFile in searchingContext.fileLineChannel) {
            LOG.finest("received lineInFile: $lineInFile")
            searchStringInLine(lineInFile.path, lineInFile.line, searchingContext.token, lineInFile.lineIndex).asFlow()
                .onEach { tokenMatch ->
                    LOG.finest("found token match in line: $tokenMatch, line: ${lineInFile.line}")
                    searchingContext.tokenMatchChannel.send(tokenMatch)
                }.collect {}
            searchingContext.searchingState.addParsedLine(lineInFile.line)
        }
        searchingContext.tokenMatchChannel.close()
        LOG.finest("closed channel tokenMatchChannel")

        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
    }

    /*For token match in tokenMatchChannel it puts this token in resultTokenMatchQueue and to token matches buffer*/
    private suspend fun asyncReadingTokenMatchesChannel(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
        for (tokenMatch in searchingContext.tokenMatchChannel) {
            LOG.finest("received tokenMatch: $tokenMatch")
            searchingContext.resultTokenMatchQueue.add(tokenMatch)
            val tokenMatchesNumber = searchingContext.searchingState.addTokenMatchToBuffer(tokenMatch)
            LOG.finest("saving #${tokenMatchesNumber} token match: $tokenMatch")
        }
        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
    }

    //TODO rewrite in concurrent way, I cannot decide how to make reduce concurrently here
    /*
    * Find all file paths, which contains all sequence char triplets from token.
    * */
    private fun getPathsByToken(trigramMap: TrigramMap, token: String): Set<Path> {
        if (token.length < 3) return emptySet()
        return (0 until token.length - 2).map { column -> token.substring(column, column + 3) }
            .map { triplet -> trigramMap.getPathsByCharTriplet(triplet) }
            .reduce { pathSet1: Set<Path>, pathSet2: Set<Path> -> pathSet1.intersect(pathSet2) }
    }

    //TODO rewrite in concurrent way, maybe divide by batches
    /*
    * Searches token by single line and creates list of tokenMatches.
    * */
    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): Sequence<TokenMatch> {
        LOG.finest("#$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(filePath, lineIndex.toLong() + 1, it.toLong() + 1) }
    }
}