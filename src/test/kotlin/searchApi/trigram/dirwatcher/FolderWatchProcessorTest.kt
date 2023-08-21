package searchApi.trigram.dirwatcher

import impl.trigram.dirwatcher.FileChangeListener
import impl.trigram.dirwatcher.FolderWatchProcessor
import impl.trigram.dirwatcher.LogFileChangeListener
import impl.trigram.dirwatcher.WatcherHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import searchApi.common.commonSetup
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Testing combination FolderWatchProcessor and WatcherHolder with simple FileChangeListener
 * */
//@Ignore
class FolderWatchProcessorTest {
    private val commonFolder: Path = commonSetup.commonPath

    /**
     * Using unique temp folder name to cut influence from different tests
     * */
    private val indexFolder: Path = commonFolder.resolve("tempFolder${UUID.randomUUID()}")

    @BeforeTest
    fun init() {
        indexFolder.toFile().mkdir()
    }

    @AfterTest
    fun finalize() {
        val file = indexFolder.toFile()
        if (file.exists()) {
            file.deleteRecursively()
        }
    }

    /**
     * Basic test to see how FolderWatchProcessor works.
     * Results of listening events are in logs
     * */
    @Test
    fun simpleTest() {
        val watcherHolder = WatcherHolder()
        val fileChangeListener = LogFileChangeListener()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val file: File = indexFolder.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file.writeText("abcdefg")

        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            val eventProcessingJob = launch {
                folderWatchProcessor.asyncProcessEvents(fileChangeListener)
            }
            launch {
                repeat(1) {
                    delay(100)
                    val textFromFile = file.readText()
                    file.delete()
                    delay(100)
                    file.writeText(textFromFile)
                    delay(100)
                    val changedText = textFromFile.reversed()
                    file.writeText(changedText)
                    delay(100)
                }
                watcherHolder.cleanUp()
                eventProcessingJob.cancel()
            }
        }
    }

    /**
     * Checking receiving events for modifying 3 times files
     * */
    @Test
    fun modify3TimesSameFileTest() {
        val watcherHolder = WatcherHolder()
        val fileChangeListener = SaveInListFileChangeListener()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val filePath = indexFolder.resolve("a").resolve("a.txt")
        val file: File = filePath.toFile().also { it.parentFile.mkdirs() }
        file.writeText("abcdefg")

        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch {
                folderWatchProcessor.asyncProcessEvents(fileChangeListener)
            }
            launch {
                delay(10)
                repeat(3) {
                    val textFromFile = file.readText()
                    val changedText = textFromFile.reversed()
                    file.writeText(changedText)
                    delay(10)
                }
                watcherHolder.cleanUp()
            }
        }
        assertAll(
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.createdFiles, "no created files") },
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.deletedFiles, "no deleted files") },
            {
                Assertions.assertEquals(
                    /* expected = */ generateSequence { filePath }.take(3).toList(),
                    /* actual = */ fileChangeListener.modifiedFiles,
                    /* message = */ "3 same modified files"
                )
            },
        )
    }

    /**
     * Checking receiving events for deleting 3 files
     *
     * Strangely it generates also 3 modifying events
     * */
    @Test
    fun deleting3FilesTest() {
        val watcherHolder = WatcherHolder()
        val fileChangeListener = SaveInListFileChangeListener()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val innerFolderPath = indexFolder.resolve("a")
        innerFolderPath.toFile().mkdir()
        val filePath1 = innerFolderPath.resolve("a.txt")
        val filePath2 = innerFolderPath.resolve("b.txt")
        val filePath3 = innerFolderPath.resolve("c.txt")
        val filePaths = listOf(filePath1, filePath2, filePath3)
        filePaths.forEach { it.toFile().createNewFile() }

        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch {
                folderWatchProcessor.asyncProcessEvents(fileChangeListener)
            }
            launch {
                delay(10)
                for (path in filePaths) {
                    path.toFile().delete()
                    delay(10)
                }
                watcherHolder.cleanUp()
            }
        }
        assertAll(
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.createdFiles, "no created files") },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeListener.modifiedFiles,
                    /* message = */ "3 same modified files"
                )
            },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeListener.deletedFiles,
                    /* message = */ "3 deleted files"
                )
            },
        )
    }

    /**
     * Checking receiving events for creating 3 files
     * */
    @Test
    fun creating3FilesTest() {
        val watcherHolder = WatcherHolder()
        val fileChangeListener = SaveInListFileChangeListener()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val innerFolderPath = indexFolder.resolve("a")
        innerFolderPath.toFile().mkdir()
        val filePath1 = innerFolderPath.resolve("a.txt")
        val filePath2 = innerFolderPath.resolve("b.txt")
        val filePath3 = innerFolderPath.resolve("c.txt")
        val filePaths = listOf(filePath1, filePath2, filePath3)
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch { folderWatchProcessor.asyncProcessEvents(fileChangeListener) }
            launch {
                delay(10)
                filePath1.toFile().createNewFile()
                delay(10)
                filePath2.toFile().createNewFile()
                delay(10)
                filePath3.toFile().createNewFile()
                delay(10)
                watcherHolder.cleanUp()
            }
        }
        assertAll(
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.deletedFiles, "no deleted files") },
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.modifiedFiles, "no modified files") },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeListener.createdFiles,
                    /* message = */ "3 created files"
                )
            },
        )
    }

    /**
     * Checking reusage fileChangeListener, folderWatchProcessor and watcherHolder with setup and cleanup
     * Same actions causes same result
     * */
    @Test
    fun twoSessionsTest() {
        val innerFolderPath = indexFolder.resolve("a")
        innerFolderPath.toFile().mkdir()
        val filePath = innerFolderPath.resolve("a.txt")

        val watcherHolder = WatcherHolder()
        val fileChangeListener = SaveInListFileChangeListener()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)
        //session 1

        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch { folderWatchProcessor.asyncProcessEvents(fileChangeListener) }
            launch {
                delay(10)
                filePath.toFile().createNewFile()
                delay(10)
                filePath.writeText("aaaa")
                delay(10)
                filePath.toFile().delete()
                delay(10)
                watcherHolder.cleanUp()
            }
        }
        val created1 = fileChangeListener.createdFiles.clone()
        val modified1 = fileChangeListener.modifiedFiles.clone()
        val deleted1 = fileChangeListener.deletedFiles.clone()
        fileChangeListener.cleanAll()

        //session 2
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch { folderWatchProcessor.asyncProcessEvents(fileChangeListener) }
            launch {
                delay(10)
                filePath.toFile().createNewFile()
                delay(10)
                filePath.writeText("aaaa")
                delay(10)
                filePath.toFile().delete()
                delay(10)
                watcherHolder.cleanUp()
            }
        }
        val created2 = fileChangeListener.createdFiles
        val modified2 = fileChangeListener.modifiedFiles
        val deleted2 = fileChangeListener.deletedFiles

        assertAll(
            { Assertions.assertEquals(created1, created2, "created files are the same in 2 sessions") },
            { Assertions.assertEquals(modified1, modified2, "modified files are the same in 2 sessions") },
            { Assertions.assertEquals(deleted1, deleted2, "deleted files are the same in 2 session") }
        )
    }

    /**
     * Checking receiving events for creating 3 files from new inner folder
     * */
    @Test
    fun newInnerFolderCreating3FilesTest() {
        val watcherHolder = WatcherHolder()
        val fileChangeListener = SaveInListFileChangeListener()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val innerFolderPath1 = indexFolder.resolve("a")
        val innerFolderPath2 = indexFolder.resolve("b")
        innerFolderPath1.toFile().mkdir()
        val filePath0 = innerFolderPath1.resolve("a.txt")
        filePath0.toFile().createNewFile()
        //innerFolderPath2 is not created yet
        val filePath1 = innerFolderPath2.resolve("a.txt")
        val filePath2 = innerFolderPath2.resolve("b.txt")
        val filePath3 = innerFolderPath2.resolve("c.txt")
        val filePaths = listOf(filePath1, filePath2, filePath3)
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch { folderWatchProcessor.asyncProcessEvents(fileChangeListener) }
            launch {
                delay(10)
                innerFolderPath2.toFile().mkdir()
                delay(10)
                filePath1.toFile().createNewFile()
                delay(10)
                filePath2.toFile().createNewFile()
                delay(10)
                filePath3.toFile().createNewFile()
                delay(10)
                watcherHolder.cleanUp()
            }
        }
        assertAll(
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.deletedFiles, "no deleted files") },
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.modifiedFiles, "no modified files") },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeListener.createdFiles,
                    /* message = */ "3 created files"
                )
            },
        )
    }

    /**
     * Checking receiving events for deleting 3 files from inner folder, which was deleted with 3 files inside
     * */
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun deletingInnerFolderWith3FilesTest() {
        val watcherHolder = WatcherHolder()
        val fileChangeListener = SaveInListFileChangeListener()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val innerFolderPath1 = indexFolder.resolve("a")
        innerFolderPath1.toFile().mkdir()
        val filePath0 = innerFolderPath1.resolve("a.txt")
        filePath0.toFile().createNewFile()

        val innerFolderPath2 = indexFolder.resolve("b")
        innerFolderPath2.toFile().mkdir()
        val filePath1 = innerFolderPath2.resolve("a.txt")
        val filePath2 = innerFolderPath2.resolve("b.txt")
        val filePath3 = innerFolderPath2.resolve("c.txt")
        val filePaths = listOf(filePath1, filePath2, filePath3)
        filePaths.forEach { it.toFile().createNewFile() }

        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch { folderWatchProcessor.asyncProcessEvents(fileChangeListener) }
            launch {
                delay(10)
                innerFolderPath2.deleteRecursively()
                delay(10)
                watcherHolder.cleanUp()
            }
        }
        assertAll(
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.createdFiles, "no created files") },
            { Assertions.assertEquals(emptyList<Path>(), fileChangeListener.modifiedFiles, "no modified files") },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeListener.deletedFiles,
                    /* message = */ "3 deleted files"
                )
            },
        )
    }

    class SaveInListFileChangeListener : FileChangeListener {
        val modifiedFiles = ArrayList<Path>()
        val createdFiles = ArrayList<Path>()
        val deletedFiles = ArrayList<Path>()

        override fun fileCreated(folder: Path, filePath: Path) {
            createdFiles.add(filePath)
        }

        override fun fileDeleted(folder: Path, filePath: Path) {
            deletedFiles.add(filePath)
        }

        override fun fileModified(folder: Path, filePath: Path) {
            modifiedFiles.add(filePath)
        }

        fun cleanAll() {
            modifiedFiles.clear()
            createdFiles.clear()
            deletedFiles.clear()
        }
    }

}