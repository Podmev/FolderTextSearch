package impl.trigram.map

import api.exception.RuntimeSearchException
import utils.WithLogging
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.getLastModifiedTime

/**
 * Structure for saving index, based on saving set of file paths where it can be found 3 sequencial characters
 * Here: charTriplet is 3 sequencial characters, like "abc", "d3d", "213"
 * Used single flow to put everything in one thread
 *
 *
 * */
class TimedTrigramMap : TrigramMap, WithLogging() {
    private val pathsByTriplet: MutableMap<String, MutableSet<Path>> = HashMap()
    private val tripletsByPath: MutableMap<Path, MutableSet<String>> = HashMap()
    private val timeByPath: MutableMap<Path, FileTime> = HashMap()

    /**
     * Add new char triplet to structure with file path containing it.
     * */
    override fun addCharTripletByPath(charTriplet: String, path: Path) {
        validateCharTriplet(charTriplet)
        addCharTripletByPathToPathsByTriplet(charTriplet, path)
        addCharTripletByPathToTripletsByPath(charTriplet, path)
        LOG.info("add by triplet \"$charTriplet\" new path: $path")
    }

    /**
     * Get all file paths containing char triplet
     * */
    override fun getPathsByCharTriplet(charTriplet: String): Set<Path> {
        validateCharTriplet(charTriplet)
        return pathsByTriplet[charTriplet] ?: emptySet()
    }

    //OPTIMIZE
    /**
     * Deep cloning map with recreated sets
     * */
    override fun clonePathsByTripletsMap(): Map<String, Set<Path>> = buildMap {
        for ((triplet, paths) in pathsByTriplet)
            put(
                key = triplet,
                value = buildSet {
                    for (path in paths) add(path)
                }
            )
    }

    /**
     * Registes modification time of path, actual at current moment
     * */
    override fun registerPathTime(path: Path) {
        val modifiedTime: FileTime = path.getLastModifiedTime()
        timeByPath[path] = modifiedTime
    }

    /**
     * Gets modification time of path saved here before
     * */
    override fun getRegisteredPathTime(path: Path): FileTime? = timeByPath[path]

    override fun addAllCharTripletsByPathAndRegisterTime(charTriplets: Set<String>, path: Path) {
        LOG.info("started")
        timeByPath[path] = path.getLastModifiedTime()
        tripletsByPath[path] = charTriplets.toMutableSet()
        for (charTriplet in charTriplets) {
            val foundPaths: MutableSet<Path>? = pathsByTriplet[charTriplet]
            if (foundPaths != null) {
                foundPaths.add(path)
            } else {
                pathsByTriplet[charTriplet] = mutableSetOf(path)
            }
        }
        LOG.info("finished with $pathsByTriplet, $tripletsByPath")
    }

    override fun removeAllCharTripletsByPathAndUnregisterTime(path: Path) {
        LOG.info("started")
        timeByPath.remove(path)
        val oldTriplets = tripletsByPath[path]
        if (oldTriplets != null) {
            for (charTriplet in oldTriplets.iterator()) {
                pathsByTriplet[charTriplet]?.remove(path)
            }
        }
        tripletsByPath.remove(path)
        LOG.info("finished with $pathsByTriplet, $tripletsByPath")
    }

    private fun addCharTripletByPathToPathsByTriplet(
        charTriplet: String, path: Path
    ) {
        val existingPathsWithTriplet: MutableSet<Path>? = pathsByTriplet[charTriplet]
        if (existingPathsWithTriplet != null) {
            val isAdded = existingPathsWithTriplet.add(path)
            if (isAdded) {
                LOG.info("add by triplet \"$charTriplet\" (now total ${existingPathsWithTriplet.size}) new path: $path")
            } else {
                LOG.info("duplicate by triplet \"$charTriplet\" (saved before) path $path")
            }
            return
        }
        pathsByTriplet[charTriplet] = mutableSetOf(path)
    }

    private fun addCharTripletByPathToTripletsByPath(
        charTriplet: String, path: Path
    ) {
        val existingTripletsWithPath: MutableSet<String>? = tripletsByPath[path]
        if (existingTripletsWithPath != null) {
            val isAdded = existingTripletsWithPath.add(charTriplet)
            if (isAdded) {
                LOG.info("add new triplet \"$charTriplet\" (now total ${existingTripletsWithPath.size}) by path: $path")
            } else {
                LOG.info("duplicate triplet \"$charTriplet\" (saved before) by path $path")
            }
            return
        }
        tripletsByPath[path] = mutableSetOf(charTriplet)
    }

    private fun validateCharTriplet(charTriplet: String) {
        if (charTriplet.length != 3) {
            throw RuntimeSearchException("Char triplet does have 3 characters, but ${charTriplet.length} $charTriplet")
        }
    }
}