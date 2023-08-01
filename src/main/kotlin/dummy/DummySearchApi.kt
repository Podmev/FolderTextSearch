package dummy

import api.*
import java.io.File
import java.util.concurrent.CompletableFuture

/*Dummy implementation of Search Api without indexing and any optimizations
  Can be used as etalon to check results, but not for performance and flexibility
* */
class DummySearchApi : SearchApi {
    /*in this implementation index is empty, so even no files are added*/
    override fun createIndexAtFolder(folderPath: String): IndexingState {
        val completableFuture = CompletableFuture<List<String>>()
        completableFuture.complete(emptyList())
        return DummyIndexingState(completableFuture)
    }

    override fun searchString(folderPath: String, token: String, settings: SearchSettings): SearchingState {
        val completableFuture = CompletableFuture<List<TokenMatch>>()
        //TODO put in async code search
        val tokenMatches = searchStringInFolder(folderPath, token)
        completableFuture.complete(tokenMatches)
        return DummySearchingState(completableFuture)
    }

    private fun searchStringInFolder(folderPath: String, token: String): List<TokenMatch> {
        println("searchStringInFolder: $folderPath, token: $token")
        val file = File(folderPath)
        if (!file.isDirectory) {
            return emptyList()
        }
        val childrenFiles = file.listFiles()!!.toList().filterNotNull()
        val tokenMatches = ArrayList<TokenMatch>()
        for (childFile in childrenFiles) {
            when {
                childFile.isDirectory -> {
                    tokenMatches.addAll(searchStringInFolder(childFile.path, token))
                }

                childFile.isFile -> {
                    tokenMatches.addAll(searchStringInFile(childFile.path, token))
                }
            }
        }
        return tokenMatches
    }

    private fun searchStringInFile(filePath: String, token: String): List<TokenMatch> {
        println("searchStringInFile: $filePath, token: $token")
        val file = File(filePath)
        val lines = file.readLines()
        val tokenMatches = ArrayList<TokenMatch>()
        for (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            tokenMatches.addAll(searchStringInLine(filePath, line, token, lineIndex))
        }
        if (tokenMatches.isEmpty()) {
            return emptyList()
        }
        return tokenMatches
    }

    private fun searchStringInLine(filePath: String, line: String, token: String, lineIndex: Int): List<TokenMatch> {
        println("searchStringInLine: #$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(filePath, lineIndex.toLong(), it.toLong()) }
    }
}

fun String?.indicesOf(token: String, ignoreCase: Boolean = false): List<Int> {
    return this?.let {
        val indexes = mutableListOf<Int>()
        var startIndex = 0
        while (startIndex in indices) {
            val index = this.indexOf(token, startIndex, ignoreCase)
            startIndex = if (index != -1) {
                indexes.add(index)
                index + 1
            } else {
                index
            }
        }
        return indexes
    } ?: emptyList()
}