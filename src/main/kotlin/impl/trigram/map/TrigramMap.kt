package impl.trigram.map

import java.nio.file.Path

/**
 * Abstract structure for saving index for one folder,
 * based on saving set of file paths where it can be found 3 sequencial characters
 * Here: charTriplet is 3 sequencial characters, like "abc", "d3d", "213"
 * Can be several implementations
 */
interface TrigramMap {

    /**
     * Add new char triplet to structure with file path containing it.
     * */
    fun addCharTripletByPath(charTriplet: String, path: Path)

    /**
     * Get all file paths containing char triplet
     * */
    fun getPathsByCharTriplet(charTriplet: String): Set<Path>

    /**
     * Deep cloning paths by char triplets map with recreated sets
     * */
    fun clonePathsByTripletsMap(): Map<String, Set<Path>>
}