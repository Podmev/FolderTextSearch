package trigram

import api.exception.RuntimeSearchException
import utils.WithLogging
import java.nio.file.Path

/*TODO add support:
*  - timestamp
*  - reuse parts of trees for different folders
*  - think about many copies of same records by file
* */

/*use flow to put everything in one thread
*
* */
class TrigramMap : WithLogging() {
    private val map: MutableMap<String, MutableSet<Path>> = HashMap()

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
    fun cloneMap() = buildMap {
        for ((triplet, paths) in map)
            put(
                key = triplet,
                value = buildSet {
                    for (path in paths) add(path)
                }
            )
    }

}