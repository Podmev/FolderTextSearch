package impl.trigram.dirwatcher

import utils.WithLogging
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * Structure that encapsulates data for watching files for different trees of files:
 *
 *  - watcherService
 *  - map from folder (tree root) to inner map from watchKey to innerFolder
 *  - map from watchKey to pair:folder(tree root) and innerFolder
 * */
class WatcherHolder : WithLogging() {
    private var innerWatcher: WatchService = createNewWatcherService()

    /**
     * Watcher for listening file events.
     * It should be recreated for every next session of listening.
     * */
    val watcher: WatchService
        get() = innerWatcher

    /**
     * Map from watchKey to pair:folder(tree root) and innerFolder.
     * Pair - (main folder, inner folder)
     * */
    private val watchMapByKey: MutableMap<WatchKey, Pair<Path, Path>> = mutableMapOf()

    /**
     * Map from folder (tree root) to inner map from watchKey to innerFolder.
     * Key - main folder, inner item - inner folder.
     * */
    private val watchMapByFolder: MutableMap<Path, MutableMap<WatchKey, Path>> = mutableMapOf()

    /**
     * Setup state before session of listening.
     * */
    fun setup() {
        closeWatcher() //mostly for seconds time and after.
        recreateWatcher()
    }

    /**
     * Clean up state after session of listening.
     * */
    fun cleanUp() {
        removeAllWatches()
        closeWatcher()
    }

    /**
     * Adds folder (tree root) to structure. Creates watchKey for each subfolder.
     * Everything gets saved in maps.
     * */
    fun addWatch(folder: Path): Boolean {
        LOG.finest("started for path: $folder")
        if (hasWatchByFolder(folder)) return false
        LOG.finest("registering: $folder")
        registerAll(folder)
        LOG.finest("after registering all")
        LOG.finest("watchMapByKey: ${watchMapByKey.size}: $watchMapByKey")
        LOG.finest("watchMapByFolder: ${watchMapByFolder.size}: $watchMapByFolder")
        return true
    }

    /**
     * Gets the pair of main folder and inner folder by watchKey and null if it is not found.
     * */
    fun getPathAndSubPath(watchKey: WatchKey): Pair<Path, Path>? = watchMapByKey[watchKey]

    /**
     * Removes watchKey and associated entries from maps.
     * */
    fun removeWatch(folder: Path): Boolean {
        val innerMap = watchMapByFolder[folder] ?: return false
        for (key in innerMap.keys) {
            key.cancel()
            watchMapByKey.remove(key)
        }
        watchMapByFolder.remove(folder)
        return true
    }


    /**
     * Removes all watchKeys and associated entries from maps.
     * After this method all maps are empty.
     * */
    fun removeAllWatches() {
        for ((watchKey, _) in watchMapByKey) {
            watchKey.cancel()
        }
        watchMapByKey.clear()
        watchMapByFolder.clear()
    }

    /**
     * Closes watcher to stop session of listening
     * */
    private fun closeWatcher() {
        innerWatcher.close()
    }

    /**
     * Recreates watcher
     * */
    private fun recreateWatcher() {
        innerWatcher = createNewWatcherService()
    }

    /**
     * Checks if the folder is registered.
     * */
    private fun hasWatchByFolder(folder: Path): Boolean = watchMapByFolder.contains(folder)

    /**
     * Creates new watcher.
     * */
    private fun createNewWatcherService() = FileSystems.getDefault().newWatchService()


    /**
     * Register inner folder of folder (can be several layers).
     * Generates one watchKey and saves associated data in maps.
     * */
    private fun register(folder: Path, innerFolder: Path) {
        val watchKey: WatchKey = registerWatch(innerFolder, watcher)
        saveToWatchMapByKey(watchKey, folder, innerFolder)
        saveToWatchMapByFolder(watchKey, folder, innerFolder)
    }

    /**
     * Saves watchKey, innerFolder and folder in watchMapByKey.
     * */
    private fun saveToWatchMapByKey(watchKey: WatchKey, folder: Path, innerFolder: Path) {
        watchMapByKey[watchKey] = Pair(folder, innerFolder)
    }

    /**
     * Saves watchKey, innerFolder and folder in watchMapByFolder.
     * */
    private fun saveToWatchMapByFolder(watchKey: WatchKey, folder: Path, innerFolder: Path) {
        val innerMap: MutableMap<WatchKey, Path>? = watchMapByFolder[folder]
        if (innerMap != null) {
            innerMap[watchKey] = innerFolder
            return
        }
        watchMapByFolder[folder] = mutableMapOf(Pair(watchKey, innerFolder))
    }


    /**
     * Registers folder recursively by walking only subfolders.
     * */
    @Throws(IOException::class)
    private fun registerAll(start: Path) {
        // register folder and sub-folders
        Files.walkFileTree(start, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(folder: Path, attrs: BasicFileAttributes): FileVisitResult {
                LOG.finest("preVisit folder: $folder")
                register(start, folder)
                return FileVisitResult.CONTINUE
            }
        })
    }

    /**
     * Registers folder in watcher for events: create, modify and delete.
     * Returns watchKey, which will be used in maps and listening process.
     * */
    private fun registerWatch(folder: Path, watcher: WatchService): WatchKey {
        return try {
            folder.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
        } catch (x: IOException) {
            System.err.println(x)
            throw x
        }
    }
}