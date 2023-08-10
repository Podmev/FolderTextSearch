package impl.trigram

import api.exception.RuntimeSearchException
import utils.WithLogging
import java.nio.file.Path

/*TODO add support:
*  - timestamp
*  - reuse parts of trees for different folders
*  - think about many copies of same records by file
* */

/**
 * Structure for saving index, based on saving set of file paths where it can be found 3 sequencial characters
 * Here: charTriplet is 3 sequencial characters, like "abc", "d3d", "213"
 * Used single flow to put everything in one thread
 * */
class TrigramMap : WithLogging() {
    private val map: MutableMap<String, MutableSet<Path>> = HashMap()

    /**
     * Add new char triplet to structure with file path containing it.
     * */
    fun addCharTripletByPath(charTriplet: String, path: Path) {
        validateCharTriplet(charTriplet)

        val existingPathsWithTriplet: MutableSet<Path>? = map[charTriplet]
        if (existingPathsWithTriplet != null) {
            val isAdded = existingPathsWithTriplet.add(path)
            if (isAdded) {
                LOG.finest("add by triplet \"$charTriplet\" (now total ${existingPathsWithTriplet.size}) new path: $path")
            } else {
                LOG.finest("duplicate by triplet \"$charTriplet\" (saved before) path $path")
            }
            return
        }
        map[charTriplet] = mutableSetOf(path)
        LOG.finest("add by triplet \"$charTriplet\" new path: $path")

    }

    /**
     * Get all file paths containing char triplet
     * */
    fun getPathsByCharTriplet(charTriplet: String): Set<Path> {
        validateCharTriplet(charTriplet)
        return map[charTriplet] ?: emptySet()
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
    fun cloneMap(): Map<String, Set<Path>> = buildMap {
        for ((triplet, paths) in map)
            put(
                key = triplet,
                value = buildSet {
                    for (path in paths) add(path)
                }
            )
    }

}