package api

interface SearchResult {
    val fileMatches: List<FileMatch>
    val totalTokenMatches: Int
}