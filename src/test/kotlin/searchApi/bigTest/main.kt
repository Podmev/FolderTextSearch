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
 * better to use JVM options -Xms256m -Xmx8192m
 * ```
 * IntellijIdeaTrigramTest().justIndex()
 * IntellijIdeaTrigramTest().justIndexWithCancel()
 * IntellijIdeaTrigramTest().indexWithSearchOneToken()
 * IntellijIdeaTrigramTest().indexWithSearchManyTokens()
 * IntellijIdeaTrigramTest().searchOneTokenAfterIndex()
 * IntellijIdeaTrigramTest().searchOneTokenAfterIndexGrepFormat()
 * IntellijIdeaTrigramTest().searchOneTokenAfterIndexWithSimpleTrigramMap()
 * ```
 * */
fun main() {
    IntellijIdeaTrigramTest().searchOneTokenAfterIndexGrepFormat()
}