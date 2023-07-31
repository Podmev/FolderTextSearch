package api

data class FileMatch(
    val filePath: String,
    val tokenMatches: List<TokenMatch>
)