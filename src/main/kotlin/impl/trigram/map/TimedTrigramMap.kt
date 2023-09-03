package impl.trigram.map

import utils.WithLogging
import utils.copy
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.getLastModifiedTime

/**
 * Structure for saving index, based on saving set of file paths where it can be found 3 sequencial characters
 * Here: charTriplet is 3 sequencial characters, like "abc", "d3d", "213"
 * Used single flow to put everything in one thread in regular indexing
 * To exclude interfering of search and incremental indexing TimedTrigramMap is partly synchronized.
 *
 * Not synchronized parts are only addCharTripletByPath for optimization of indexing,
 * which is separated by design from search and incremental indexing.
 *
 * There is some shared code between SimpleTrigramMap and TimedTrigramMap, but not enough to make common base class
 * */
class TimedTrigramMap : TrigramMap, WithLogging() {
    private val pathsByTriplet: MutableMap<String, MutableSet<Path>> = HashMap()
    private val tripletsByPath: MutableMap<Path, MutableSet<String>> = HashMap()
    private val timeByPath: MutableMap<Path, FileTime> = HashMap()

    /**
     * Add new char triplet to structure with file path containing it.
     * Not synchronized, because it is used in regular indexing,
     * and it is separated from searching and incremental indexing
     * */
    override fun addCharTripletByPath(charTriplet: String, path: Path) {
        validateCharTriplet(charTriplet)
        addCharTripletByPathToPathsByTriplet(charTriplet, path)
        addCharTripletByPathToTripletsByPath(charTriplet, path)
        LOG.finest("add by triplet \"$charTriplet\" new path: $path")
    }

    /**
     * Get all file paths containing char triplet
     * Synchronized to exclude situation interfering with incremental indexing
     * (addAllCharTripletsByPathAndRegisterTime, removeAllCharTripletsByPathAndUnregisterTime)
     * Also cloning set from pathsByTriplet to exclude changes from other places
     * */
    @Synchronized
    override fun getPathsByCharTriplet(charTriplet: String): Set<Path> {
        validateCharTriplet(charTriplet)
        return pathsByTriplet[charTriplet]?.toSet() ?: emptySet()
    }

    /**
     * Deep cloning map with recreated sets
     * Synchronized to exclude situation interfering with incremental indexing
     * (addAllCharTripletsByPathAndRegisterTime, removeAllCharTripletsByPathAndUnregisterTime)
     * */
    @Synchronized
    override fun clonePathsByTripletsMap(): Map<String, Set<Path>> = pathsByTriplet.copy()

    /**
     * Registes modification time of path, actual at current moment
     * Not synchronized, because it is used in regular indexing,
     * and it is separated from searching and incremental indexing
     * */
    override fun registerPathTime(path: Path, lastModificationTime: FileTime) {
        timeByPath[path] = lastModificationTime
    }

    /**
     * Gets modification time of path saved here before
     *
     * No need to be synchronized - not invoked while changing TimedTrigramMap
     * */
    override fun getRegisteredPathTime(path: Path): FileTime? = timeByPath[path]

    /**
     * No need to be synchronized - not invoked while changing TimedTrigramMap
     * */
    override fun getAllRegisteredPathsWithTime(): List<Pair<Path, FileTime>> = timeByPath.toList()

    /**
     * No need to be synchronized - not invoked while changing TimedTrigramMap
     * */
    override fun getAllRegisteredPaths(): List<Path> = timeByPath.keys.toList()

    /**
     * Synchronized to exclude situation interfering with search (getPathsByCharTriplet)
     * */
    @Synchronized
    override fun addAllCharTripletsByPathAndRegisterTime(charTriplets: Set<String>, path: Path) {
        LOG.finest("started")
        timeByPath[path] = path.getLastModifiedTime()
        tripletsByPath[path] = charTriplets.toMutableSet()
        for (charTriplet in charTriplets) {
            val foundPaths: MutableSet<Path> = pathsByTriplet.computeIfAbsent(charTriplet) { mutableSetOf() }
            foundPaths.add(path)
        }
        LOG.finest("finished with $pathsByTriplet, $tripletsByPath")
    }

    /**
     * Synchronized to exclude situation interfering with search (getPathsByCharTriplet)
     * */
    @Synchronized
    override fun removeAllCharTripletsByPathAndUnregisterTime(path: Path) {
        LOG.finest("started")
        timeByPath.remove(path)
        val oldTriplets = tripletsByPath[path]
        if (oldTriplets != null) {
            for (charTriplet in oldTriplets) {
                pathsByTriplet[charTriplet]?.remove(path)
            }
        }
        tripletsByPath.remove(path)
        LOG.finest("finished with $pathsByTriplet, $tripletsByPath")
    }

    private fun addCharTripletByPathToPathsByTriplet(
        charTriplet: String, path: Path
    ) {

        val existingPathsWithTriplet: MutableSet<Path> = pathsByTriplet.computeIfAbsent(charTriplet) { mutableSetOf() }
        val isAdded = existingPathsWithTriplet.add(path)
        if (isAdded) {
            LOG.finest("add by triplet \"$charTriplet\" (now total ${existingPathsWithTriplet.size}) new path: $path")
        } else {
            LOG.finest("duplicate by triplet \"$charTriplet\" (saved before) path $path")
        }
    }

    private fun addCharTripletByPathToTripletsByPath(
        charTriplet: String, path: Path
    ) {
        val existingTripletsWithPath: MutableSet<String> = tripletsByPath.computeIfAbsent(path) { mutableSetOf() }
        val isAdded = existingTripletsWithPath.add(charTriplet)
        if (isAdded) {
            LOG.finest("add new triplet \"$charTriplet\" (now total ${existingTripletsWithPath.size}) by path: $path")
        } else {
            LOG.finest("duplicate triplet \"$charTriplet\" (saved before) by path $path")
        }
    }

}