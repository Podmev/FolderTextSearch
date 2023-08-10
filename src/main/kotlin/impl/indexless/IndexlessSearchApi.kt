package impl.indexless

import api.*
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import utils.WithLogging
import utils.indicesOf
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines
import kotlin.streams.asSequence

/**
 * Indexless implementation of Search Api without indexing and any optimizations
 * Can be used as etalon to check search results, but not for performance and flexibility
 * */
class IndexlessSearchApi : SearchApi, WithLogging() {
    /**
     * In this implementation index is empty, so even no files are added.
     * */
    override fun createIndexAtFolder(folderPath: Path): IndexingState {
        val completableFuture = CompletableFuture<List<Path>>()
        completableFuture.complete(emptyList())
        return IndexlessIndexingState(completableFuture)
    }

    /**
     * Naive straight-forward implementation to search tokens without any index
     * */
    override fun searchString(folderPath: Path, token: String, settings: SearchSettings): SearchingState {
        validateToken(token)
        validatePath(folderPath)
        val completableFuture = CompletableFuture<List<TokenMatch>>()
        //TODO put in async code search
        val tokenMatches = searchStringInFolder(folderPath, token)
        completableFuture.complete(tokenMatches)
        return IndexlessSearchingState(completableFuture)
    }

    /**
     * This implementation never has index at any folder. So it is false.
     * */
    override fun hasIndexAtFolder(folderPath: Path): Boolean = false

    /**
     * Cannot remove index at folder in this implementation.
     * */
    override fun removeIndexAtFolder(folderPath: Path): Boolean = false

    /**
     * Nothing to remove in this implementation.
     * */
    override fun removeFullIndex() { /*nothing to do* */
    }

    /**
     * This implementation never has index at any folder. So it is empty list.
     * */
    override fun getAllIndexedFolders(): List<Path> = emptyList()

    private fun validateToken(token: String) {
        if (token.length < 3) {
            throw IllegalArgumentSearchException("Token is too small, it has length less than 3 characters.")
        }
        for (forbiddenChar in forbiddenCharsInToken) {
            if (token.contains(forbiddenChar)) {
                throw IllegalArgumentSearchException("Token has forbidden character")
            }
        }
    }

    private fun validatePath(folderPath: Path) {
        if (!folderPath.isDirectory()) {
            throw NotDirSearchException(folderPath)
        }
    }

    private fun searchStringInFolder(folderPath: Path, token: String): List<TokenMatch> =
        Files.walk(folderPath).use {
            it.asSequence()
                .filter { path -> path.isRegularFile() }
                .flatMap { file -> searchStringInFile(file, token) }
                .toList()
        }

    private fun searchStringInFile(filePath: Path, token: String): List<TokenMatch> =
        filePath.useLines { lines ->
            lines
                .flatMapIndexed { lineIndex, line -> searchStringInLine(filePath, line, token, lineIndex) }
                .toList()
        }

    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): Sequence<TokenMatch> {
        LOG.info("#$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(filePath, lineIndex.toLong() + 1, it.toLong() + 1) }
    }

    companion object {
        private val forbiddenCharsInToken: List<Char> = listOf('\n', '\r')
    }
}
