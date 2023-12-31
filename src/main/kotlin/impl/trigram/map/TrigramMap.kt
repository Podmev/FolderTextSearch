package impl.trigram.map

import java.nio.file.Path
import java.nio.file.attribute.FileTime

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

    /**
     * Registes modification time of path, actual at current moment
     * */
    fun registerPathTime(path: Path, lastModificationTime: FileTime)

    /**
     * Gets modification time of path saved here before
     * */
    fun getRegisteredPathTime(path: Path): FileTime?

    /**
     * Gets list of all registered files with time
     * */
    fun getAllRegisteredPathsWithTime(): List<Pair<Path, FileTime>>

    /**
     * Gets list of all registered files
     * */
    fun getAllRegisteredPaths(): List<Path>

    /**
     * Bulk operation for adding all char triplets by path, and register file
     * */
    fun addAllCharTripletsByPathAndRegisterTime(charTriplets: Set<String>, path: Path)

    /**
     * Bulk operation for removing all char triplets by path, and unregister file
     * */
    fun removeAllCharTripletsByPathAndUnregisterTime(path: Path)

}