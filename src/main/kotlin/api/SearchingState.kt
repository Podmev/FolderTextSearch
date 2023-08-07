package api

import java.nio.file.Path
import java.util.concurrent.Future

//TODO add inner index state for cases when we don't have index
/*State which api gives when you make search of token at folder.
* It can help to control process of searching, since it can be long process
* */
interface SearchingState {
    //TODO make state more complex - check index, indexing, searching, finish, failed
    /*Shows if search finished*/
    val finished: Boolean

    /*can be from 0 till 1 inclusive borders, where 0 means not started, and 1  - finished*/
    val progress: Double

    /*result, which will be fill in the end of search*/
    val result: Future<List<TokenMatch>>

    /*method to cancel the search process.
     It can be useful, if it takes long time*/
    fun cancel()

    /*Get the newfound portion of tokenMatches after previous call.
    * You can set flush true, if you don't want to save current buffer value for next time.
    * Otherwise, buffer will grow till the end, and it will be equals result.
    * */
    fun getTokenMatchesBuffer(flush: Boolean): List<TokenMatch>

    /*Get the newfound portion of file paths visited after previous call.
    * You can set flush true, if you don't want to save current buffer value for next time.
    * Otherwise, buffer will grow till the end, and it will be equals result.
    * */
    fun getVisitedPathsBuffer(flush: Boolean): List<Path>

    /*Number of visited files
    * */
    val visitedFilesNumber: Long

    /*Total size of files to search according to created index.
    * Value updates once after finishing walking all files dedicated to search by index.
    * At first, it is null.
    * */
    val totalFilesNumber: Long?

    /*Size in bytes of visited files during search.
    * Value updates all the time. After successful finishing should be equal totalFilesByteSize
    * */
    val visitedFilesByteSize: Long

    /*Size in bytes of parse files during search.
    * Value updates all the time. After successful finishing should close to totalFilesByteSize
    * */
    val parsedFilesByteSize: Long

    /*Total size in bytes of searched files in folder.
    * Value updates once after finishing walking all files dedicated to search by index.
    * At first, it is null.
    * */
    val totalFilesByteSize: Long?
}

/*Takes immutable snapshot of SearchingState*/
fun SearchingState.toSnapshot(): SearchingStateSnapshot =
    SearchingStateSnapshot(
        finished = finished,
        progress = progress,
        visitedPathsBuffer = getVisitedPathsBuffer(true),
        tokenMatchesBuffer = getTokenMatchesBuffer(true),
        visitedFilesNumber = visitedFilesNumber,
        totalFilesNumber = totalFilesNumber,
        visitedFilesByteSize = visitedFilesByteSize,
        parsedFilesByteSize = parsedFilesByteSize,
        totalFilesByteSize = totalFilesByteSize
    )