package dummy

import api.*
import java.io.File

/*Dummy implementation of Search Api without indexing and any optimizations
  Can be used as etalon to check results, but not for performance and flexibility
* */
class DummySearchApi() : SearchApi {
    override fun createIndexAtFolder(folderPath: String): IndexingState = DummyIndexingState()

    override fun searchString(folderPath: String, token: String, settings: SearchSettings): SearchingState {
        val state = DummySearchingState()
        val fileMatches = searchStringInFolder(folderPath, token)
        val totalTokenMatches = fileMatches.sumOf { it.tokenMatches.size }
        val searchResult = DummySearchResult(fileMatches, totalTokenMatches)
        state.setCurrentSearchResult(searchResult)
        return state
    }

    private fun searchStringInFolder(folderPath: String, token: String): List<FileMatch> {
        println("searchStringInFolder: $folderPath, token: $token")
        val file = File(folderPath)
        if (!file.isDirectory) {
            return emptyList()
        }
        val childrenFiles = file.listFiles()!!.toList().filterNotNull()
        val fileMatches = ArrayList<FileMatch>()
        for (childFile in childrenFiles) {
            when {
                childFile.isDirectory -> {
                    fileMatches.addAll(searchStringInFolder(childFile.path, token))
                }

                childFile.isFile -> {
                    fileMatches.addAll(searchStringInFile(childFile.path, token))
                }
            }
        }
        return fileMatches
    }

    private fun searchStringInFile(filePath: String, token: String): List<FileMatch> {
        println("searchStringInFile: $filePath, token: $token")
        val file = File(filePath)
        val lines = file.readLines()
        val tokenMatches = ArrayList<TokenMatch>()
        for (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            tokenMatches.addAll(searchStringInLine(line, token, lineIndex))
        }
        if (tokenMatches.isEmpty()) {
            return emptyList()
        }
        return listOf(FileMatch(filePath, tokenMatches))
    }

    private fun searchStringInLine(line: String, token: String, lineIndex: Int): List<TokenMatch> {
        println("searchStringInLine: #$lineIndex, \"$line\", token: $token")
        val positionsInLine = line.indicesOf(token)
        return positionsInLine.map { TokenMatch(lineIndex.toLong(), it.toLong()) }
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