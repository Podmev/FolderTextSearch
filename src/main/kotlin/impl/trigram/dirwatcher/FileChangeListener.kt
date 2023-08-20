package impl.trigram.dirwatcher

import utils.WithLogging
import java.nio.file.Path

/**
 * Interface, that serves to set up actions on event for files: created, modified, deleted
 * */
interface FileChangeListener {
    /**
     * Action to do, when new file is created
     * */
    fun fileCreated(folder: Path, filePath: Path)

    /**
     * Action to do, when file is deleted
     * */
    fun fileDeleted(folder: Path, filePath: Path)

    /**
     * Action to do, when file is modified
     * */
    fun fileModified(folder: Path, filePath: Path)
}

/**
 * Simple implementation of FileChangeListener with just logging events
 * */
class LogFileChangeListener : FileChangeListener, WithLogging() {
    override fun fileCreated(folder: Path, filePath: Path) {
        LOG.finest("Event created file $filePath in folder $folder")
    }

    override fun fileDeleted(folder: Path, filePath: Path) {
        LOG.finest("Event deleted file $filePath in folder $folder")
    }

    override fun fileModified(folder: Path, filePath: Path) {
        LOG.finest("Event modified file $filePath in folder $folder")
    }

}