package impl.trigram.dirwatcher

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import utils.WithLogging
import utils.coroutines.makeCancelablePoint
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

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
    suspend fun asyncProcessEvents(fileChangeListener: FileChangeListener) = coroutineScope {
        try {
            LOG.finest("started")
            LOG.finest("started while(true)")
            while (true) {
                LOG.finest("started another iteration while(true)")
                if (!isActive) return@coroutineScope
                LOG.finest("before makeCancelablePoint")
                makeCancelablePoint()
                LOG.finest("before taking key")
                // wait for key to be signaled
                val key: WatchKey = watcherHolder.watcher.take()
                if (!processEventsByWatchKey(key, fileChangeListener)) {
                    LOG.finest("couldn't process watch key - so we go to next iteration")
                    continue
                }
                LOG.finest("after successfully processing watch key")
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
        fileChangeListener: FileChangeListener
    ): Boolean {
        val pathAndSubPath = watcherHolder.getPathAndSubPath(key)
        if (pathAndSubPath == null) {
            LOG.severe("WatchKey not recognized!!")
            return false
        }
        val (folder: Path, innerFolder: Path) = pathAndSubPath
        LOG.finest("folder:$folder, innerFolder:$innerFolder")

        for (event in key.pollEvents()) {
            makeCancelablePoint()
            processEvent(folder, innerFolder, event, fileChangeListener)
        }
        return true
    }

    /**
     * Processes single event for one watchKey.
     *  - Takes kind of event, if it is overflow, we skip
     *  - Takes fileName from event - it is context of event
     *  - If filename is not plain text, we ignore it
     *  - Constructs file path by inner folder and fileName
     *  - Depending on kind of event it fires one of three methods of FileChangeListener
     * */
    @Suppress("UNCHECKED_CAST")
    private fun processEvent(
        folder: Path, innerFolder: Path, event: WatchEvent<*>, fileChangeListener: FileChangeListener
    ) {
        val kind: WatchEvent.Kind<out Any> = event.kind()
        // This key is registered only for ENTRY_CREATE events,
        // but an OVERFLOW event can occur regardless if events are lost or discarded.
        if (kind === OVERFLOW) return
        // The filename is the context of the event.
        val filename: Path = (event as WatchEvent<Path>).context()


        val filePath = innerFolder.resolve(filename)
        LOG.finest("filename:${filename} is directory: ${filePath.isDirectory()}")
        when {
            filePath.isDirectory() -> processDirectoryEvent(kind, folder, filePath, fileChangeListener)
            else -> processRegularFileEvent(kind, folder, filePath, fileChangeListener)
        }
        LOG.finest("changes $kind of file: folder: $folder, innerFolder:$folder, filename:$filename")
    }

    private fun processDirectoryEvent(
        kind: WatchEvent.Kind<out Any>, folder: Path, innerFolderPath: Path, fileChangeListener: FileChangeListener
    ) {
        LOG.finest("start processing directory $kind for $innerFolderPath")
        when (kind) {
            ENTRY_CREATE -> {
                watcherHolder.addWatch(folder, innerFolderPath)

                Files.walkFileTree(innerFolderPath, object : SimpleFileVisitor<Path>() {
                    @Throws(IOException::class)
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        LOG.finest("sending created file from new subfolder: $innerFolderPath: $file")
                        if (file.isRegularFile()) {
                            fileChangeListener.fileCreated(folder, file)
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
            }

            ENTRY_DELETE -> {/*deleted directory is not interesting  */
            }

            ENTRY_MODIFY -> {/*modified directory is not interesting*/
            }
        }
    }

    private fun processRegularFileEvent(
        kind: WatchEvent.Kind<out Any>, folder: Path, filePath: Path, fileChangeListener: FileChangeListener
    ) {
        LOG.finest("start processing regular file $kind for $filePath")
        when (kind) {
            ENTRY_CREATE -> fileChangeListener.fileCreated(folder, filePath)
            ENTRY_DELETE -> fileChangeListener.fileDeleted(folder, filePath)
            ENTRY_MODIFY -> fileChangeListener.fileModified(folder, filePath)
        }
    }

}