package impl.trigram.incremental

import java.nio.file.Path

/**
 * Event about changing file within folder
 * */
data class FileEvent(
    /**
     * Kind of event - modified, created or deleted file
     * */
    val kind: Kind,
    /**
     * Indexing folder, where file has changed
     * */
    val folder: Path,
    /**
     * Relative path to changed file
     * */
    val filePath: Path
) {

    /**
     * Kind of event - modified, created or deleted file
     * */
    enum class Kind {

        /**
         * Modified file event kind
         */
        MODIFIED,

        /**
         * Created file event kind
         */
        CREATED,

        /**
         * Deleted file event kind
         */
        DELETED
    }
}