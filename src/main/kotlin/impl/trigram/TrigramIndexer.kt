package impl.trigram

import api.ProgressableStatus
import impl.trigram.map.TrigramMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import utils.WithLogging
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.stream.Stream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines
import kotlin.streams.asSequence

/**
 * Only logic of constructing index for TrigramSearApi
 * */
internal class TrigramIndexer(
    val createNewTrigramMap: () -> TrigramMap, val addPathFolderWatch: (Path) -> Unit
) : WithLogging() {
    /**
     * Main logic of indexing, checks if index is created already.
     * If there is no index, it starts indexing.
     * Has 4 processes:
     *  - walking files,
     *  - indexing files,
     *  - reading paths of already indexed files to save progress,
     *  - reading found triplets in each file to save in trigramMap,
     * In the end it fills result list of indexed files in CompletableFuture object
     *
     * It can be called only one per time, TrigramSearchApi controls it
     *
     * @param finalize: function to run before setting up future
     * */
    suspend fun asyncIndexing(
        folderPath: Path,
        future: CompletableFuture<List<Path>>,
        indexingState: TrigramIndexingState,
        trigramMapByFolder: MutableMap<Path, TrigramMap>,
        finalize: () -> Unit
    ): TrigramMap = coroutineScope {
        try {
            indexingState.changeStatus(ProgressableStatus.IN_PROGRESS)
            LOG.finest("started for folder: $folderPath")
            val resultPathQueue: Queue<Path> = LinkedBlockingQueue()
            val foundTrigramMap: TrigramMap? = trigramMapByFolder[folderPath]
            var resultTrigramMap = foundTrigramMap
            coroutineScope {
                if (foundTrigramMap == null) {
                    //Here we don't worry about other thread calling the same method - TrigramSearchApi allows only 1 per time
                    val trigramMap = createNewTrigramMap()
                    LOG.finest("created new trigramMap for folder $folderPath")
                    val indexingContext = TrigramIndexingContext(folderPath, indexingState, resultPathQueue, trigramMap)
                    // Here we need extra scope to wait make actions after - all jobs will be finished
                    coroutineScope {
                        launch { asyncWalkingFiles(indexingContext) }
                        launch { asyncIndexingFiles(indexingContext) }
                        launch { asyncReadingIndexedPathChannel(indexingContext) }
                        launch { asyncReadingTripletInPathChannel(indexingContext) }
                    }
                    trigramMapByFolder[folderPath] = trigramMap
                    resultTrigramMap = trigramMap
                    // Here we already register watch for folder for incremental indexing
                    addPathFolderWatch(folderPath)
                }
            }
            //here we wait all coroutines to finish
            val resultPathList = resultPathQueue.toList()
            indexingState.changeStatus(ProgressableStatus.FINISHED)
            finalize()
            future.complete(resultPathList)
            LOG.finest("finished for folder: $folderPath, indexed ${resultPathList.size} files")
            return@coroutineScope resultTrigramMap!!
        } catch (ex: CancellationException) {
            indexingState.changeStatus(ProgressableStatus.CANCELLED)
            finalize()
            future.complete(emptyList())
            throw ex // Must let the CancellationException propagate
        } catch (th: Throwable) {
            indexingState.changeStatus(ProgressableStatus.FAILED)
            indexingState.setFailReason(th)
            finalize()
            future.complete(emptyList())
            LOG.severe("exception during making index: ${th.message}")
            th.printStackTrace()
            throw th
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
            for (path in it.asSequence().filter { path -> path.isRegularFile() }) {
                LOG.finest("visiting file by path $path")
                indexingContext.visitedPathChannel.send(path)
                val visitedFilesNumber = indexingContext.indexingState.addVisitedPathToBuffer(path)
                LOG.finest("successfully visited $visitedFilesNumber")
            }
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
    ) {
        LOG.finest("started for folder: ${indexingContext.folderPath}")

        // we need a coroutineScope to wait for all coroutines to finish
        coroutineScope {
            for (path in indexingContext.visitedPathChannel) {
                if (!isActive) break
                //all indices are run independently
                launch {
                    val successful = constructIndexForFile(path, indexingContext)
                    if (!successful) LOG.finest("skipped file $path")
                    indexingContext.indexedPathChannel.trySendBlocking(Pair(path, path.getLastModifiedTime()))
                        .onSuccess { LOG.finest("send successfully path $path to indexedPathChannel") }
                        .onFailure { t: Throwable? -> LOG.severe("Cannot send path in indexedPathChannel: ${t?.message}") }
                }
            }
        }

        //close channel only after all indexing coroutines are finished
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
                lines.forEachIndexed { lineIndex, line ->
                    constructIndexForLine(
                        path = path, line = line, lineIndex = lineIndex, indexingContext = indexingContext
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
        path: Path, line: String, lineIndex: Int, indexingContext: TrigramIndexingContext
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
        for ((path, modificationTime) in indexingContext.indexedPathChannel) {
            if (!isActive) break
            LOG.finest("received indexed path and saving to state: $path")
            val indexedFilesNumber = indexingContext.indexingState.addIndexedPathToBuffer(path)
            indexingContext.resultPathQueue.add(path)

            //using modification time of file on moment before starting reading and not after
            indexingContext.trigramMap.registerPathTime(path, modificationTime)
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