package impl.trigram

import api.ProgressableStatus
import api.TokenMatch
import impl.trigram.map.TrigramMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import utils.WithLogging
import utils.coroutines.makeCancelablePoint
import utils.indicesOf
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.useLines

/**
 * Only logic of searching token using constructed index for TrigramSearApi
 * */
class TrigramSearcher : WithLogging() {

    /**
     * Searches asynchronously for token in folder with using constructed index trigramMap
     * */
    suspend fun asyncSearching(
        folderPath: Path,
        token: String,
        trigramMap: TrigramMap,
        future: CompletableFuture<List<TokenMatch>>,
        searchingState: TrigramSearchingState,
    ) = coroutineScope {
        try {
            searchingState.changeStatus(ProgressableStatus.IN_PROGRESS)
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
                }
            }
            //here we wait all coroutines to finish
            val resultPathList = resultTokenMatchQueue.toList()
            searchingState.changeStatus(ProgressableStatus.FINISHED)
            future.complete(resultPathList)
            LOG.finest("finished for folder: $folderPath and token: \"$token\", ${resultPathList.size} token matches")
        } catch (ex: CancellationException) {
            searchingState.changeStatus(ProgressableStatus.CANCELLED)
            future.complete(emptyList())
            throw ex // Must let the CancellationException propagate
        } catch (th: Throwable) {
            searchingState.changeStatus(ProgressableStatus.FAILED)
            searchingState.setFailReason(th)
            future.complete(emptyList())
            LOG.severe("exception during making index: ${th.message}")
            th.printStackTrace()
        }
    }

    /**
     * Takes narrowed paths for token - only those files which has ALL together triplets of sequencial character from token
     * For each path it sends in channel narrowedPathChannel
     * */
    private suspend fun asyncWalkTokenAndNarrowPaths(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"${searchingContext.token}\"")

        val narrowedPaths = getPathsByToken(searchingContext.trigramMap, searchingContext.token)
        LOG.finest("got ${narrowedPaths.size} narrowed paths from trigramMap by token \"${searchingContext.token}\"")
        makeCancelablePoint()

        narrowedPaths.forEach {
            makeCancelablePoint()
            searchingContext.searchingState.addVisitedPath(it)
        }
        searchingContext.searchingState.setTotalFilesByteSize()
        searchingContext.searchingState.setTotalFilesNumber()

        for (path in narrowedPaths.asSequence()) {
            searchingContext.narrowedPathChannel.send(path)
            LOG.finest("sent path to channel narrowedPathChannel: $path, isActive:$isActive")
        }

        searchingContext.narrowedPathChannel.close()
        LOG.finest("closed channel narrowedPathChannel")

        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"${searchingContext.token}\"")
    }

    /**
     * For each path in narrowedPathChannel it sends all lines of file in channel fileLineChannel.
     * */
    private suspend fun asyncSearchingInPaths(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")

        //running in parallel
        coroutineScope {
            for (path in searchingContext.narrowedPathChannel) {
                if (!isActive) break
                LOG.finest("received path: $path, isActive:$isActive")
                launch {
                    path.useLines { lines ->
                        lines.forEachIndexed { lineIndex, line ->
                            searchingContext.fileLineChannel.send(LineInFile(path, lineIndex, line))
                        }
                    }
                }
            }
        }
        searchingContext.fileLineChannel.close()
        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
    }

    /**
     * For each line in fileLineChannel it sends all found token matches to tokenMatchChannel.
     * */
    private suspend fun asyncSearchingInFileLines(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\", isActive:$isActive")

        //running in parallel
        coroutineScope {
            for (lineInFile in searchingContext.fileLineChannel) {
                if (!isActive) break
                LOG.finest("received lineInFile: $lineInFile, isActive:$isActive")
                launch {
                    val tokenMatches: Sequence<TokenMatch> = searchStringInLine(
                        filePath = lineInFile.path,
                        line = lineInFile.line,
                        token = searchingContext.token,
                        lineIndex = lineInFile.lineIndex
                    )
                    for (tokenMatch in tokenMatches) {
                        LOG.finest("found token match in line: $tokenMatch, line: ${lineInFile.line}, isActive:$isActive")
                        searchingContext.tokenMatchChannel.send(tokenMatch)
                    }
                    searchingContext.searchingState.addParsedLine(lineInFile.line)
                }
            }
        }
        searchingContext.tokenMatchChannel.close()
        LOG.finest("closed channel tokenMatchChannel")

        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
    }

    /**
     * For token match in tokenMatchChannel it puts this token in resultTokenMatchQueue and to token matches buffer.
     * */
    private suspend fun asyncReadingTokenMatchesChannel(searchingContext: TrigramSearchingContext) = coroutineScope {
        LOG.finest("started for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
        for (tokenMatch in searchingContext.tokenMatchChannel) {
            if (!isActive) break
            LOG.finest("received tokenMatch: $tokenMatch, isActive:$isActive")
            searchingContext.resultTokenMatchQueue.add(tokenMatch)
            val tokenMatchesNumber = searchingContext.searchingState.addTokenMatchToBuffer(tokenMatch)
            LOG.finest("saving #${tokenMatchesNumber} token match: $tokenMatch, isActive:$isActive")
        }
        LOG.finest("finished for folder: ${searchingContext.folderPath} and token: \"$searchingContext.token\"")
    }

    /**
     * Find all file paths, which contains all sequence char triplets from token.
     * */
    private fun getPathsByToken(trigramMap: TrigramMap, token: String): Set<Path> {
        if (token.length < 3) return emptySet()
        return (0 until token.length - 2)
            .asSequence()// asSequence to avoid unnecessary intermediate lists
            .map { column -> token.substring(column, column + 3) }
            .map { triplet -> trigramMap.getPathsByCharTriplet(triplet) }
            .reduce { pathSet1: Set<Path>, pathSet2: Set<Path> -> pathSet1.intersect(pathSet2) }
    }

    /**
     * Searches token by single line and creates list of tokenMatches.
     * Also converts to 1-based indices.
     * */
    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): Sequence<TokenMatch> {
        LOG.finest("#$lineIndex, \"$line\", token: $token")
        val positionsInLine: Sequence<Int> = line.indicesOf(token)
        return positionsInLine
            .map { TokenMatch(filePath, lineIndex.toLong() + 1, it.toLong() + 1) }
    }
}