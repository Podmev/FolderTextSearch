package searchApi.bigTest

/**
 * Entry point to test big tests in CurProjectTrigramTest and IntellijIdeaTrigramTest
 *
 *
 * CurProjectTrigramTest:
 * ```
 * CurProjectTrigramTest().justIndex()
 * CurProjectTrigramTest().indexWithSearchOneToken()
 * CurProjectTrigramTest().searchOneTokenAfterIndex()
 * ```
 * IntellijIdeaTrigramTest:
 * ```
 * IntellijIdeaTrigramTest().justIndex()
 * IntellijIdeaTrigramTest().justIndexWithCancel()
 * IntellijIdeaTrigramTest().indexWithSearchOneToken()
 * IntellijIdeaTrigramTest().indexWithSearchManyTokens()
 * IntellijIdeaTrigramTest().searchOneTokenAfterIndex()
 * ```
 * */
fun main() {
    IntellijIdeaTrigramTest().searchOneTokenAfterIndex()
}