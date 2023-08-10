package searchApi.common

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

/**
 * Global consts for tests
 * */
object CommonSetup {
    private val projectPath: Path = Paths.get("")
    val commonPath: Path =
        projectPath.resolve("src").resolve("test").resolve("resources").resolve("searchTestFolders")

    val srcFolder: Path = projectPath.resolve("src")

    val intellijIdeaProjectPath: Path = projectPath.absolute().parent.resolve("intellij-community")

}
