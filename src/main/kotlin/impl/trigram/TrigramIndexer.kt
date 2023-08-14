package impl.trigram

import api.ProgressableStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import utils.WithLogging
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines
import kotlin.streams.asSequence

/**
 * Only logic of constructing index for TrigramSearApi
 * */
internal class TrigramIndexer : WithLogging() {
    /**
     * Main logic of indexing, checks if index is created already.
     * If there is no index, it starts indexing.
     * Has 4 processes:
     *  - walking files,
     *  - indexing files,
     *  - reading paths of already indexed files to save progress,
     *  - reading found triplets in each file to save in trigramMap,
     * In the end it fills result list of indexed files in CompletableFuture object
     * */
    suspend fun asyncIndexing(
        folderPath: Path,
        future: CompletableFuture<List<Path>>,
        indexingState: TrigramIndexingState,
        trigramMapByFolder: MutableMap<Path, TrigramMap>,
        indexInProcess: AtomicBoolean
    ) = coroutineScope {
        try {
            indexingState.changeStatus(ProgressableStatus.IN_PROGRESS)
            LOG.finest("started for folder: $folderPath")
            val resultPathQueue: Queue<Path> = LinkedBlockingQueue()
            val foundTrigramMap: TrigramMap? = trigramMapByFolder[folderPath]
            coroutineScope {
                if (foundTrigramMap == null) {
                    val trigramMap = TrigramMap()
                    LOG.finest("created new trigramMap for folder $folderPath")
                    val indexingContext = TrigramIndexingContext(folderPath, indexingState, resultPathQueue, trigramMap)
                    coroutineScope {
                        launch { asyncWalkingFiles(indexingContext) }
                        launch { asyncIndexingFiles(indexingContext) }
                        launch { asyncReadingIndexedPathChannel(indexingContext) }
                        launch { asyncReadingTripletInPathChannel(indexingContext) }
                    }
                    trigramMapByFolder[folderPath] = trigramMap
                }
            }
            //here we wait all coroutines to finish
            val resultPathList = resultPathQueue.toList()
            indexingState.changeStatus(ProgressableStatus.FINISHED)
            indexInProcess.set(false)
            future.complete(resultPathList)
            LOG.finest("finished for folder: $folderPath, indexed ${resultPathList.size} files")
        } catch (ex: CancellationException) {
            indexingState.changeStatus(ProgressableStatus.CANCELLED)
            indexInProcess.set(false)
            future.complete(emptyList())
            throw ex // Must let the CancellationException propagate
        } catch (th: Throwable) {
            indexingState.changeStatus(ProgressableStatus.FAILED)
            indexingState.setFailReason(th)
            indexInProcess.set(false)
            future.complete(emptyList())
            LOG.severe("exception during making index: ${th.message}")
            th.printStackTrace()
        }
    }

    /**
     * Walks files: on each file - increment counter and send path in visited path channel.
     * In the end it closes visited path channel.
     * */
    private suspend fun asyncWalkingFiles(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started for folder: ${indexingContext.folderPath}")

        withContext(Dispatchers.IO) {
            Files.walk(indexingContext.folderPath)
        }.use { it: Stream<Path> ->
            it.asSequence()
                .filter { path -> path.isRegularFile() }
                .asFlow().onEach { path ->
                    LOG.finest("visiting file by path $path")
                    indexingContext.visitedPathChannel.send(path)
                    val visitedFilesNumber = indexingContext.indexingState.addVisitedPathToBuffer(path)
                    LOG.finest("successfully visited $visitedFilesNumber")
                }.collect { }
        }

        indexingContext.visitedPathChannel.close()
        LOG.finest("closed visitedPathChannel")

        val totalFilesNumber = indexingContext.indexingState.visitedFilesNumber
        val setupSuccessful = indexingContext.indexingState.setTotalFilesNumber(totalFilesNumber)
        LOG.finest("setup totalFilesNumber (successfully: $setupSuccessful) in indexing state: $totalFilesNumber")

        LOG.finest("finished for folder: ${indexingContext.folderPath}")
    }

    /**
     * On each visited path from visited path channel.
     * It constructs index for file (only if it is possible) and sends file path to indexed path channel.
     * In the end it closes 2 channels: indexed path channel and triplet in path channel.
     * */
    private suspend fun asyncIndexingFiles(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started for folder: ${indexingContext.folderPath}")

        for (path in indexingContext.visitedPathChannel) {
            if (!isActive) break
            val successful = constructIndexForFile(path, indexingContext)
            if (!successful) LOG.finest("skipped file $path")
            indexingContext.indexedPathChannel.trySendBlocking(path)
                .onSuccess { LOG.finest("send successfully path $path to indexedPathChannel") }
                .onFailure { t: Throwable? -> LOG.severe("Cannot send path in indexedPathChannel: ${t?.message}") }
        }
        indexingContext.indexedPathChannel.close()
        LOG.finest("closed indexedPathChannel")
        indexingContext.tripletInPathChannel.close()
        LOG.finest("closed tripletInPathChannel")
        LOG.finest("finished for folder: ${indexingContext.folderPath}")
    }

    /**
     * Lazy for each line separately constructs index.
     * */
    private suspend fun constructIndexForFile(path: Path, indexingContext: TrigramIndexingContext): Boolean {
        try {
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
            return true
        } catch (ex: CancellationException) {
            throw ex // Must let the CancellationException propagate
        } catch (ex: MalformedInputException) {
            //Broken on reading file - skipping it
            return false
        } catch (th: Throwable) {
            LOG.severe("exception during constructing index for file ${path}: ${th.message}")
            th.printStackTrace()
            throw th
        }
    }

    /**
     * Runs with sliding window of 3 characters and sends to triplet in path channel.
     * */
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

    /**
     * Reads indexed files, updates state and resultList.
     * */
    private suspend fun asyncReadingIndexedPathChannel(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started")
        for (path in indexingContext.indexedPathChannel) {
            if (!isActive) break
            LOG.finest("received indexed path and saving to state: $path")
            val indexedFilesNumber = indexingContext.indexingState.addIndexedPathToBuffer(path)
            indexingContext.resultPathQueue.add(path)
            LOG.finest("successfully indexed $indexedFilesNumber files")
        }
        LOG.finest("finished")
    }

    /**
     * Reads triplets of chars by path in channel, saves in trigramMap and updates state.
     * */
    private suspend fun asyncReadingTripletInPathChannel(
        indexingContext: TrigramIndexingContext
    ) = coroutineScope {
        LOG.finest("started")
        for ((triplet, path) in indexingContext.tripletInPathChannel) {
            if (!isActive) break
            LOG.finest("received path for triplet: $triplet $path")
            indexingContext.trigramMap.addCharTripletByPath(triplet, path)
        }
        LOG.finest("finished")
    }

}