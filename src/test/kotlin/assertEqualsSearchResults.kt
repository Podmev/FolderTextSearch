import api.SearchResult
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertAll

/*Detailed assert for SearchResult with no exact order of files in search result.
* If error happens, shows exact mismatch.
* */
fun assertEqualsSearchResults(expectedSearchResult: SearchResult, actualSearchResult: SearchResult) {
    val expectedFileMap = expectedSearchResult.fileMatches.associate { Pair(it.filePath, it.tokenMatches) }
    val actualFileMap = actualSearchResult.fileMatches.associate { Pair(it.filePath, it.tokenMatches) }
    val expectedFiles = expectedFileMap.keys
    val actualFiles = actualFileMap.keys
    val commonFiles = expectedFiles.intersect(actualFiles)
    assertAll(
        "compare search results",
        { ->
            Assertions.assertEquals(
                expectedSearchResult.totalTokenMatches,
                actualSearchResult.totalTokenMatches,
                "Compare totalTokenMatches"
            )
        },
        { ->
            Assertions.assertEquals(
                expectedFiles.minus(actualFiles),
                emptySet<String>(),
                "Not found some expected files in actual"
            )
        },
        { ->
            Assertions.assertEquals(
                actualFiles.minus(expectedFiles),
                emptySet<String>(),
                "Found some extra files in actual, rather than expected"
            )
        },
        *commonFiles.map {
            { ->
                Assertions.assertEquals(
                    expectedFileMap[it],
                    actualFileMap[it],
                    "Compare file matches in file $it"
                )
            }
        }.toTypedArray()
    )
}