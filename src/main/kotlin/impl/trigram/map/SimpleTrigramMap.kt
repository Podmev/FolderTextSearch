package impl.trigram.map

import api.exception.RuntimeSearchException
import utils.WithLogging
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

        val existingPathsWithTriplet: MutableSet<Path>? = pathsByTriplets[charTriplet]
        if (existingPathsWithTriplet != null) {
            val isAdded = existingPathsWithTriplet.add(path)
            if (isAdded) {
                LOG.finest("add by triplet \"$charTriplet\" (now total ${existingPathsWithTriplet.size}) new path: $path")
            } else {
                LOG.finest("duplicate by triplet \"$charTriplet\" (saved before) path $path")
            }
            return
        }
        pathsByTriplets[charTriplet] = mutableSetOf(path)
        LOG.finest("add by triplet \"$charTriplet\" new path: $path")

    }

    /**
     * Get all file paths containing char triplet
     * */
    override fun getPathsByCharTriplet(charTriplet: String): Set<Path> {
        validateCharTriplet(charTriplet)
        return pathsByTriplets[charTriplet] ?: emptySet()
    }

    private fun validateCharTriplet(charTriplet: String) {
        if (charTriplet.length != 3) {
            throw RuntimeSearchException("Char triplet does have 3 characters, but ${charTriplet.length} $charTriplet")
        }
    }

    //OPTIMIZE
    /**
     * Deep cloning map with recreated sets
     * */
    override fun clonePathsByTripletsMap(): Map<String, Set<Path>> = buildMap {
        for ((triplet, paths) in pathsByTriplets)
            put(
                key = triplet,
                value = buildSet {
                    for (path in paths) add(path)
                }
            )
    }

    override fun registerPathTime(path: Path) {
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