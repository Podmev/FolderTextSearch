package impl.trigram.dirwatcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import utils.WithLogging
import utils.coroutines.makeCancelablePoint
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*

/**
 * Service to process events files from registered folders
 * WatcherService, WatchKey, maps for paths are all in WatcherHolder
 *
 * Used documentation from https://docs.oracle.com/javase/tutorial/essential/io/notification.html
 * */
class FolderWatchProcessor(
    private val watcherHolder: WatcherHolder
) : WithLogging() {

    /**
     * Process events from files in infinite loop
     * */
    suspend fun asyncProcessEvents(fileChangeReactor: FileChangeReactor) = coroutineScope {
        try {
            LOG.finest("started")
            while (true) {
                LOG.finest("started while(true)")
                if (!isActive) return@coroutineScope
                LOG.finest("before makeCancelablePoint")
                makeCancelablePoint()
                LOG.finest("before taking key")
                // wait for key to be signaled
                val key: WatchKey = withContext(Dispatchers.IO) { watcherHolder.watcher.take() }
                if (processEventsByWatchKey(key, fileChangeReactor)) continue
                // Reset the key -- this step is critical if you want to
                // receive further watch events.  If the key is no longer valid,
                // the directory is inaccessible so exit the loop.
                val valid = key.reset()
                if (!valid) break
            }
        } catch (x: ClosedWatchServiceException) {
            LOG.finest("closed watcher")
            return@coroutineScope
        } catch (x: InterruptedException) {
            LOG.severe("Interrupted exception")
            return@coroutineScope
        }
    }

    /**
     * Processes all events for one watchKey from WatcherService.
     * Takes path of folder and inner folder associated with watchKey from watcherHolder.
     * */
    private suspend fun processEventsByWatchKey(
        key: WatchKey,
        fileChangeReactor: FileChangeReactor
    ): Boolean {
        val pathAndSubPath = watcherHolder.getPathAndSubPath(key)
        if (pathAndSubPath == null) {
            LOG.severe("WatchKey not recognized!!")
            return true
        }
        val (folder: Path, innerFolder: Path) = pathAndSubPath
        LOG.finest("folder:$folder, innerFolder:$innerFolder")

        for (event in key.pollEvents()) {
            makeCancelablePoint()
            processEvent(folder, innerFolder, event, fileChangeReactor)
        }
        return false
    }

    /**
     * Processes single event for one watchKey.
     *  - Takes kind of event, if it is overflow, we skip
     *  - Takes fileName from event - it is context of event
     *  - If filename is not plain text, we ignore it
     *  - Constructs file path by inner folder and fileName
     *  - Depending on kind of event it fires one of three methods of FileChangeReactor
     * */
    @Suppress("UNCHECKED_CAST")
    private suspend fun processEvent(
        folder: Path, innerFolder: Path, event: WatchEvent<*>, fileChangeReactor: FileChangeReactor
    ) {
        val kind: WatchEvent.Kind<out Any> = event.kind()
        // This key is registered only for ENTRY_CREATE events,
        // but an OVERFLOW event can occur regardless if events are lost or discarded.
        if (kind === OVERFLOW) return
        // The filename is the context of the event.
        val filename: Path = (event as WatchEvent<Path>).context()
        if (!checkFileIsPlain(filename, innerFolder)) return

        val filePath = innerFolder.resolve(filename)
        when (kind) {
            ENTRY_CREATE -> fileChangeReactor.reactOnCreatedFile(folder, filePath)
            ENTRY_DELETE -> fileChangeReactor.reactOnDeletedFile(folder, filePath)
            ENTRY_MODIFY -> fileChangeReactor.reactOnModifiedFile(folder, filePath)
        }
        LOG.finest("changes $kind of file: folder: $folder, innerFolder:$folder, filename:$filename")
    }

    //TODO check if it is needed
    /**
     * Verify that the new file is a text file
     * */
    private suspend fun checkFileIsPlain(filename: Path, innerFolder: Path): Boolean {
        return try {
            // Resolve the filename against the directory.
            // If the filename is "test" and the directory is "foo",
            // the resolved name is "test/foo".
            val child: Path = innerFolder.resolve(filename)
            withContext(Dispatchers.IO) {
                Files.probeContentType(child)
            } == "text/plain"
        } catch (x: IOException) {
            System.err.println(x)
            false
        }
    }

}