package searchApi.trigram.dirwatcher

import impl.trigram.dirwatcher.FileChangeReactor
import impl.trigram.dirwatcher.FolderWatchProcessor
import impl.trigram.dirwatcher.LogFileChangeReactor
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Testing combination FolderWatchProcessor and WatcherHolder with simple FileChangeReactor
 * */
class FolderWatchProcessorTest {
    private val commonFolder: Path = commonSetup.commonPath
    private val indexFolder: Path = commonFolder.resolve("tempFolder")

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
        val fileChangeReactor = LogFileChangeReactor()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val file: File = indexFolder.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file.writeText("abcdefg")

        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            val eventProcessingJob = launch {
                folderWatchProcessor.asyncProcessEvents(fileChangeReactor)
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
        val fileChangeReactor = SaveInListFileChangeReactor()
        val folderWatchProcessor = FolderWatchProcessor(watcherHolder)

        val filePath = indexFolder.resolve("a").resolve("a.txt")
        val file: File = filePath.toFile().also { it.parentFile.mkdirs() }
        file.writeText("abcdefg")

        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder)

        runBlocking {
            launch {
                folderWatchProcessor.asyncProcessEvents(fileChangeReactor)
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
            { Assertions.assertEquals(emptyList<Path>(), fileChangeReactor.createdFiles, "no created files") },
            { Assertions.assertEquals(emptyList<Path>(), fileChangeReactor.deletedFiles, "no deleted files") },
            {
                Assertions.assertEquals(
                    /* expected = */ generateSequence { filePath }.take(3).toList(),
                    /* actual = */ fileChangeReactor.modifiedFiles,
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
        val fileChangeReactor = SaveInListFileChangeReactor()
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
                folderWatchProcessor.asyncProcessEvents(fileChangeReactor)
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
            { Assertions.assertEquals(emptyList<Path>(), fileChangeReactor.createdFiles, "no created files") },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeReactor.modifiedFiles,
                    /* message = */ "3 same modified files"
                )
            },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeReactor.deletedFiles,
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
        val fileChangeReactor = SaveInListFileChangeReactor()
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
            launch {
                folderWatchProcessor.asyncProcessEvents(fileChangeReactor)
            }
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
            { Assertions.assertEquals(emptyList<Path>(), fileChangeReactor.modifiedFiles, "no modified files") },
            { Assertions.assertEquals(emptyList<Path>(), fileChangeReactor.modifiedFiles, "no modified files") },
            {
                Assertions.assertEquals(
                    /* expected = */ filePaths,
                    /* actual = */ fileChangeReactor.createdFiles,
                    /* message = */ "3 created files"
                )
            },
        )
    }

    class SaveInListFileChangeReactor : FileChangeReactor {
        val modifiedFiles = ArrayList<Path>()
        val createdFiles = ArrayList<Path>()
        val deletedFiles = ArrayList<Path>()

        override fun reactOnCreatedFile(folder: Path, filePath: Path) {
            createdFiles.add(filePath)
        }

        override fun reactOnDeletedFile(folder: Path, filePath: Path) {
            deletedFiles.add(filePath)
        }

        override fun reactOnModifiedFile(folder: Path, filePath: Path) {
            modifiedFiles.add(filePath)
        }
    }

}