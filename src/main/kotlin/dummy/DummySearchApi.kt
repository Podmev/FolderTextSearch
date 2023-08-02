package dummy

import api.*
import api.exception.NotDirSearchException
import utils.WithLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines
import kotlin.streams.asSequence

/*Dummy implementation of Search Api without indexing and any optimizations
  Can be used as etalon to check results, but not for performance and flexibility
* */
class DummySearchApi : SearchApi, WithLogging() {
    /*in this implementation index is empty, so even no files are added*/
    override fun createIndexAtFolder(folderPath: Path): IndexingState {
        val completableFuture = CompletableFuture<List<Path>>()
        completableFuture.complete(emptyList())
        return DummyIndexingState(completableFuture)
    }

    override fun searchString(folderPath: Path, token: String, settings: SearchSettings): SearchingState {
        val completableFuture = CompletableFuture<List<TokenMatch>>()
        //TODO put in async code search
        val tokenMatches = searchStringInFolder(folderPath, token)
        completableFuture.complete(tokenMatches)
        return DummySearchingState(completableFuture)
    }

    private fun searchStringInFolder(folderPath: Path, token: String): List<TokenMatch> {
        LOG.info("$folderPath, token: $token")
        if (!folderPath.isDirectory()) {
            throw NotDirSearchException(folderPath)
        }
        return Files.walk(folderPath).use {
            it.asSequence()
                .filter { path -> path.isRegularFile() }
                .flatMap { file -> searchStringInFile(file, token) }
                .toList()
        }
    }

    private fun searchStringInFile(filePath: Path, token: String): List<TokenMatch> =
        filePath.useLines { lines ->
            lines
                .flatMapIndexed { lineIndex, line -> searchStringInLine(filePath, line, token, lineIndex) }
                .toList()
        }

    private fun searchStringInLine(filePath: Path, line: String, token: String, lineIndex: Int): List<TokenMatch> {
        LOG.info("#$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(filePath, lineIndex.toLong(), it.toLong()) }.toList()
    }
}

fun String.indicesOf(token: String, ignoreCase: Boolean = false): Sequence<Int> {
    fun next(startOffset: Int) = this.indexOf(token, startOffset, ignoreCase).takeIf { it != -1 }
    return generateSequence(next(0)) { prevIndex -> next(prevIndex + 1) }
}