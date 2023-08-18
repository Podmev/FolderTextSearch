package impl.trigram.incremental

import impl.trigram.dirwatcher.FileChangeReactor
import impl.trigram.map.TrigramMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import utils.WithLogging
import utils.coroutines.makeCancelablePoint
import java.nio.charset.MalformedInputException
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.useLines

/**
 * Only logic of constructing index for TrigramSearApi
 * */
internal class TrigramIncrementalIndexer(
    trigramMapByFolder: Map<Path, TrigramMap>
) : FileChangeReactor, WithLogging() {
    private val context = TrigramIncrementalIndexingContext(trigramMapByFolder)

    override fun reactOnCreatedFile(folder: Path, filePath: Path): Unit = runBlocking {
        LOG.finest("Event created file $filePath in folder $folder")
        context.changedFileChannel.send(FileEvent(FileEvent.Kind.CREATED, folder, filePath))
    }

    override fun reactOnDeletedFile(folder: Path, filePath: Path): Unit = runBlocking {
        LOG.finest("Event deleted file $filePath in folder $folder")
        context.changedFileChannel.send(FileEvent(FileEvent.Kind.DELETED, folder, filePath))
    }

    override fun reactOnModifiedFile(folder: Path, filePath: Path): Unit = runBlocking {
        LOG.finest("Event modified file $filePath in folder $folder")
        context.changedFileChannel.send(FileEvent(FileEvent.Kind.MODIFIED, folder, filePath))
    }

    /**
     * Process all incoming file
     * */
    suspend fun asyncProcessFileChanges() = coroutineScope {
        LOG.finest("started")
        for (fileEvent in context.changedFileChannel) {
            LOG.finest("processing event $fileEvent")
            if (!isActive) return@coroutineScope
            makeCancelablePoint()

            val trigramMap: TrigramMap? = context.trigramMapByFolder[fileEvent.folder]
            if (trigramMap == null) {
                LOG.finest("not found trigram map for folder $fileEvent.folder, we skip it")
                continue
            }

            when (fileEvent.kind) {
                FileEvent.Kind.CREATED -> processCreatedFile(trigramMap, fileEvent.folder, fileEvent.filePath)
                FileEvent.Kind.MODIFIED -> processModifiedFile(trigramMap, fileEvent.folder, fileEvent.filePath)
                FileEvent.Kind.DELETED -> processDeletedFile(trigramMap, fileEvent.folder, fileEvent.filePath)
            }
        }
        LOG.finest("finished")
    }

    private fun processCreatedFile(trigramMap: TrigramMap, folder: Path, filePath: Path) {
        LOG.finest("processing created file $filePath in $folder")
        val registeredTime: FileTime? = trigramMap.getRegisteredPathTime(filePath)
        if (registeredTime != null) {
            //already have this filePath
            LOG.finest("There is already registered data for file $filePath in folder $folder in trigramMap, so we skip it")
            return
        }
        LOG.finest("checked registered time")
        val tripletSetFromFile: Set<String>? = getTripletSetFromFile(filePath)
        if (tripletSetFromFile == null) {
            LOG.finest(
                "Couldn't receive triplets from $filePath in folder $folder, " +
                        "because of bad encoding in file or not plain text, so we skip it"
            )
            return
        }
        LOG.finest("got tripletSetFromFile with size ${tripletSetFromFile.size} from $filePath in $folder")
        trigramMap.addAllCharTripletsByPathAndRegisterTime(tripletSetFromFile, filePath)
        LOG.finest("finished processing created file $filePath in $folder")
    }

    private fun processModifiedFile(trigramMap: TrigramMap, folder: Path, filePath: Path) {
        LOG.finest("processing modified file $filePath in $folder")
        val registeredTime = trigramMap.getRegisteredPathTime(filePath)
        if (registeredTime != null && registeredTime > filePath.getLastModifiedTime()) {
            //registered time is later than file path lastModification
            LOG.finest(
                "Registered time is later than file path lastModification " +
                        "of file $filePath in folder $folder in trigramMap, so we skip it"
            )
            return
        }
        processDeletedFile(trigramMap, folder, filePath)
        processCreatedFile(trigramMap, folder, filePath)
        LOG.finest("finished processing modified file $filePath in $folder")
    }

    private fun processDeletedFile(trigramMap: TrigramMap, folder: Path, filePath: Path) {
        LOG.finest("processing deleted file $filePath in $folder")
        val registeredTime = trigramMap.getRegisteredPathTime(filePath)
        if (registeredTime == null) {
            //already don't have this filePath
            LOG.finest("There is no registered data for file $filePath in folder $folder in trigramMap, so we skip it")
            return
        }
        trigramMap.removeAllCharTripletsByPathAndUnregisterTime(filePath)
        LOG.finest("finished processing deleted file $filePath in $folder")
    }

    /**
     * Gets set of char triplets from file. if file is wrong format, it returns null
     * */
    private fun getTripletSetFromFile(path: Path): Set<String>? = try {
        path.useLines { lines ->
            lines.flatMap { getAllTripletsInLine(it) }.toSet()
        }
    } catch (ex: MalformedInputException) {
        //Broken on reading file - skipping it
        null
    } catch (th: Throwable) {
        LOG.severe("exception during getting TripletSet from file ${path}: ${th.message}")
        th.printStackTrace()
        throw th
    }

    /**
     * Runs with sliding window of 3 characters and get all triplets.
     * */
    private fun getAllTripletsInLine(line: String): List<String> =
        if (line.length < 3) emptyList()
        else buildList {
            for (column in 0 until line.length - 2) {
                add(line.substring(column, column + 3))
            }
        }
}