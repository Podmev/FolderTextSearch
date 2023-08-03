package common

import java.nio.file.Path
import java.nio.file.Paths

object commonSetup {
    private val projectPath: Path = Paths.get("")
    val commonPath: Path =
        projectPath.resolve("src").resolve("test").resolve("resources").resolve("searchTestFolders")

}
