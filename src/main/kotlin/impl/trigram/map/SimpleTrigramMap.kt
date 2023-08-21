package impl.trigram.map

import utils.WithLogging
import utils.copy
import java.nio.file.Path
import java.nio.file.attribute.FileTime

/**
 * Structure for saving index, based on saving set of file paths where it can be found 3 sequencial characters
 * Here: charTriplet is 3 sequencial characters, like "abc", "d3d", "213"
 * Used single flow to put everything in one thread
 * */
class SimpleTrigramMap : TrigramMap, WithLogging() {
    private val pathsByTriplets: MutableMap<String, MutableSet<Path>> = HashMap()

    /**
     * Add new char triplet to structure with file path containing it.
     * */
    override fun addCharTripletByPath(charTriplet: String, path: Path) {
        validateCharTriplet(charTriplet)

        val existingPathsWithTriplet: MutableSet<Path> = pathsByTriplets.computeIfAbsent(charTriplet) { mutableSetOf() }
        val isAdded = existingPathsWithTriplet.add(path)
        if (isAdded) {
            LOG.finest("add by triplet \"$charTriplet\" (now total ${existingPathsWithTriplet.size}) new path: $path")
        } else {
            LOG.finest("duplicate by triplet \"$charTriplet\" (saved before) path $path")
        }
    }

    /**
     * Get all file paths containing char triplet
     * */
    override fun getPathsByCharTriplet(charTriplet: String): Set<Path> {
        validateCharTriplet(charTriplet)
        return pathsByTriplets[charTriplet] ?: emptySet()
    }

    /**
     * Deep cloning map with recreated sets
     * */
    override fun clonePathsByTripletsMap(): Map<String, Set<Path>> = pathsByTriplets.copy()

    override fun registerPathTime(path: Path, lastModificationTime: FileTime) {
        /*do nothing*/
    }

    override fun getRegisteredPathTime(path: Path): FileTime? = null

    override fun getAllRegisteredPathsWithTime(): List<Pair<Path, FileTime>> = emptyList()

    override fun getAllRegisteredPaths(): List<Path> = emptyList()

    override fun addAllCharTripletsByPathAndRegisterTime(charTriplets: Set<String>, path: Path) {
        /*do nothing*/
    }

    override fun removeAllCharTripletsByPathAndUnregisterTime(path: Path) {
        /*do nothing*/
    }

}