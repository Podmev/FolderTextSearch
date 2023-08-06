package api

import java.nio.file.Path
import java.util.concurrent.Future

//TODO add millis from start
interface IndexingState {
    /*Shows if search finished*/
    val finished: Boolean

    /*can be from 0 till 1 inclusive borders, where 0 means not started, and 1  - finished*/
    val progress: Double

    /*result - all file paths in directory recursively, which were indexed*/
    val result: Future<List<Path>>

    /*method to cancel the search process.
     It can be useful, if it takes long time*/
    fun cancel()

    /*Get the newfound portion of file paths visited after previous call.
    * You can set flush true, if you don't want to save current buffer value for next time.
    * Otherwise, buffer will grow till the end, and it will be equals result.
    * */
    fun getVisitedPathsBuffer(flush: Boolean): List<Path>

    /*Get the newfound portion of file paths analyzed (indexed) after previous call.
    * You can set flush true, if you don't want to save current buffer value for next time.
    * Otherwise, buffer will grow till the end, and it will be equals result.
    * */
    fun getIndexedPathsBuffer(flush: Boolean): List<Path>

    /*Number of visited files during indexing.
    * Value updates all the time. After successful finishing should be equal totalFiles
    * */
    val visitedFilesNumber: Long

    /*Number of indexed files during indexing.
    * Value updates all the time. After successful finishing should be equal totalFiles
    * */
    val indexedFilesNumber: Long

    /*Total number of files in folder.
    * Value updates once after finishing walking all files.
    * At first, it is null.
    * */
    val totalFilesNumber: Long?
}