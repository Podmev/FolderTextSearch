package dummy

import api.FileMatch
import api.SearchResult

data class DummySearchResult(
    override val fileMatches: List<FileMatch>,
    override val totalTokenMatches: Int
) : SearchResult