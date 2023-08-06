package impl.trigram

import api.TokenMatch
import utils.WithLogging
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines

/*Only logic of searching token using constructed index for TrigramSearApi*/
class TrigramSearcher : WithLogging() {

    /*Searches for token in folder with using constructed index trigramMap*/
    fun searchForTokenMatches(
        folderPath: Path,
        token: String,
        trigramMap: TrigramMap
    ): List<TokenMatch> {
        val paths = getPathsByToken(trigramMap, token)
        LOG.finest("got ${paths.size} paths for token $token by trigramMap in folder $folderPath: $paths")
        val tokenMatches = searchStringInPaths(paths, token)
        LOG.finest("got ${tokenMatches.size} token matches for token $token in folder $folderPath: $paths")
        return tokenMatches
    }

    /*Find all file paths, which contains all sequence char triplets from token.
    * */
    private fun getPathsByToken(trigramMap: TrigramMap, token: String): Set<Path> {
        if (token.length < 3) return emptySet()
        return (0 until token.length - 2)
            .map { column -> token.substring(column, column + 3) }
            .map { triplet -> trigramMap.getPathsByCharTriplet(triplet) }
            .reduce { pathSet1: Set<Path>, pathSet2: Set<Path> -> pathSet1.intersect(pathSet2) }
    }

    /*Searches token in paths.
    * We already know that each of them has every triple sequential characters.
    * */
    private fun searchStringInPaths(paths: Collection<Path>, token: String): List<TokenMatch> =
        paths.asSequence()
            .filter { path -> path.isRegularFile() }
            .flatMap { file -> searchStringInFile(file, token) }
            .toList()

    //TODO think if it is possible to make index for line number
    /*Searches token by single path.
    * Searches for every line separately.
    * */
    private fun searchStringInFile(filePath: Path, token: String): List<TokenMatch> =
        filePath.useLines() { lines ->
            lines
                .flatMapIndexed { lineIndex, line -> searchStringInLine(filePath, line, token, lineIndex) }
                .toList()
        }

    /*Searches token by single line and creates list of tokenMatches.
    * */
    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): List<TokenMatch> {
        LOG.finest("#$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(filePath, lineIndex.toLong() + 1, it.toLong() + 1) }.toList()
    }
}

/*Finds all indices of starts of token in line.*/
fun String.indicesOf(token: String, ignoreCase: Boolean = false): Sequence<Int> {
    fun next(startOffset: Int) = this.indexOf(token, startOffset, ignoreCase).takeIf { it != -1 }
    return generateSequence(next(0)) { prevIndex -> next(prevIndex + 1) }
}