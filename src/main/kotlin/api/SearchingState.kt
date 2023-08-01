package api

import java.util.concurrent.Future

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
    fun getBufferPartResult(flush: Boolean): List<TokenMatch>
}