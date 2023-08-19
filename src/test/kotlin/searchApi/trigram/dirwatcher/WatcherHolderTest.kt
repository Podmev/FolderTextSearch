package searchApi.trigram.dirwatcher

import impl.trigram.dirwatcher.WatcherHolder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import searchApi.common.commonSetup
import java.io.File
import java.nio.file.Path
import java.nio.file.WatchKey
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertNotNull

/**
 * Tests for logic and consistency of WatcherHolder
 * */
class WatcherHolderTest {
    private val commonFolder: Path = commonSetup.commonPath
    private val indexFolder1: Path = commonFolder.resolve("tempFolder1${UUID.randomUUID()}")
    private val indexFolder2: Path = commonFolder.resolve("tempFolder2${UUID.randomUUID()}")

    @BeforeTest
    fun init() {
        indexFolder1.toFile().mkdir()
        indexFolder2.toFile().mkdir()
    }

    @AfterTest
    fun finalize() {
        val file1 = indexFolder1.toFile()
        if (file1.exists()) {
            file1.deleteRecursively()
        }

        val file2 = indexFolder2.toFile()
        if (file2.exists()) {
            file2.deleteRecursively()
        }
    }

    /**
     * Checks that after add watch for folder in watcherHolder,
     * we have watcher for it
     * */
    @Test
    fun hasWatchForFolderAfterAddTest() {
        val watcherHolder = WatcherHolder()
        val file: File = indexFolder1.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file.writeText("abcdefg")
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder1)
        Assertions.assertTrue(watcherHolder.hasWatchByFolder(indexFolder1), "watchHolder should have registered folder")
    }

    /**
     * Checks that after add watch for folder and then remove it in watcherHolder,
     * we don't have watcher for it
     * */
    @Test
    fun noWatchForFolderAfterAddAndRemoveTest() {
        val watcherHolder = WatcherHolder()
        val file: File = indexFolder1.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file.writeText("abcdefg")
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder1)
        watcherHolder.removeWatch(indexFolder1)
        Assertions.assertFalse(
            watcherHolder.hasWatchByFolder(indexFolder1), "watchHolder should not have registered folder after removing"
        )
    }

    /**
     * Checks that after add watch for folder1 and for folder 2 and then remove it for only first in watcherHolder,
     * we don't have watcher for the first folder, but we have for the second.
     * */
    @Test
    fun add2remove1WatchTest() {
        val watcherHolder = WatcherHolder()
        val file1: File = indexFolder1.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText("abcdefg")
        val file2: File = indexFolder2.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText("abcdefg")
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder1)
        watcherHolder.addWatch(indexFolder2)
        watcherHolder.removeWatch(indexFolder1)
        assertAll(
            {
                Assertions.assertFalse(
                    watcherHolder.hasWatchByFolder(indexFolder1),
                    "watchHolder should not have registered folder1 after removing"
                )
            },
            {
                Assertions.assertTrue(
                    watcherHolder.hasWatchByFolder(indexFolder2),
                    "watchHolder should have registered folder2"
                )
            }
        )
    }

    /**
     * Checks that after add watch for folder1 and for folder 2 and then remove it for all in watcherHolder,
     * we don't have watcher for both folders.
     * */
    @Test
    fun add2removeAllWatchTest() {
        val watcherHolder = WatcherHolder()
        val file1: File = indexFolder1.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText("abcdefg")
        val file2: File = indexFolder2.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText("abcdefg")
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder1)
        watcherHolder.addWatch(indexFolder2)
        watcherHolder.removeAllWatches()
        assertAll(
            {
                Assertions.assertFalse(
                    watcherHolder.hasWatchByFolder(indexFolder1),
                    "watchHolder should not have registered folder1 after removing all watches"
                )
            },
            {
                Assertions.assertFalse(
                    watcherHolder.hasWatchByFolder(indexFolder2),
                    "watchHolder should not have registered folder2 after removing all watches"
                )
            }
        )
    }

    /**
     * After setup watcher watcherHolder has different watcher
     * */
    @Test
    fun recreateWatcherOnSetupTest() {
        val watcherHolder = WatcherHolder()
        val watcherBeforeSetup = watcherHolder.watcher
        watcherHolder.setup()
        Assertions.assertNotEquals(watcherBeforeSetup, watcherHolder.watcher, "After setup watcher changes")
    }

    /**
     * Checks that after add watch for folder1 and for folder 2 and then cleanup in watcherHolder,
     * we don't have watcher for both folders.
     * */
    @Test
    fun cleanUpAfterAdd2WatchesTest() {
        val watcherHolder = WatcherHolder()
        val file1: File = indexFolder1.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText("abcdefg")
        val file2: File = indexFolder2.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file2.writeText("abcdefg")
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder1)
        watcherHolder.addWatch(indexFolder2)
        watcherHolder.cleanUp()
        assertAll(
            {
                Assertions.assertFalse(
                    watcherHolder.hasWatchByFolder(indexFolder1),
                    "watchHolder should not have registered folder1 after removing all watches"
                )
            },
            {
                Assertions.assertFalse(
                    watcherHolder.hasWatchByFolder(indexFolder2),
                    "watchHolder should not have registered folder2 after removing all watches"
                )
            }
        )
    }

    /**
     * Checks consistency of getWatchKeysWithPath and getPathAndSubPath methods,
     * which inner folder will be saved - main and one inner.
     * */
    @Test
    fun getWatchKeysWithPathTest() {
        val watcherHolder = WatcherHolder()
        val file1: File = indexFolder1.resolve("a").resolve("a.txt").toFile().also { it.parentFile.mkdirs() }
        file1.writeText("abcdefg")
        watcherHolder.setup()
        watcherHolder.addWatch(indexFolder1)
        val watchKeyAndPathList: List<Pair<WatchKey, Path>>? = watcherHolder.getWatchKeysWithPath(indexFolder1)
        assertNotNull(watchKeyAndPathList)
        assertAll(
            { Assertions.assertEquals(2, watchKeyAndPathList.size, "there 2 watchKeys with paths") },
            {
                Assertions.assertEquals(
                    setOf(indexFolder1, indexFolder1.resolve("a")),
                    watchKeyAndPathList.map { it.second }.toSet(),
                    "there 2 inner folders, saved for watch"
                )
            },
            *buildList {
                for ((watchKey, innerFolder) in watchKeyAndPathList) {
                    add {
                        Assertions.assertEquals(
                            Pair(indexFolder1, innerFolder),
                            watcherHolder.getPathAndSubPath(watchKey)
                        )
                    }
                }
            }.toTypedArray()
        )
    }
}