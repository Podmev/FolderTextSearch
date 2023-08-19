package impl.trigram.dirwatcher

import utils.WithLogging
import java.nio.file.Path

/**
 * Interface, that serves to set up actions on event for files: created, modified, deleted
 * */
interface FileChangeReactor {
    /**
     * Action to do, when new file is created
     * */
    fun reactOnCreatedFile(folder: Path, filePath: Path)

    /**
     * Action to do, when file is deleted
     * */
    fun reactOnDeletedFile(folder: Path, filePath: Path)

    /**
     * Action to do, when file is modified
     * */
    fun reactOnModifiedFile(folder: Path, filePath: Path)
}

/**
 * Simple implementation of FileChangeReactor with just logging events
 * */
class LogFileChangeReactor : FileChangeReactor, WithLogging() {
    override fun reactOnCreatedFile(folder: Path, filePath: Path) {
        LOG.finest("Event created file $filePath in folder $folder")
    }

    override fun reactOnDeletedFile(folder: Path, filePath: Path) {
        LOG.finest("Event deleted file $filePath in folder $folder")
    }

    override fun reactOnModifiedFile(folder: Path, filePath: Path) {
        LOG.finest("Event modified file $filePath in folder $folder")
    }

}