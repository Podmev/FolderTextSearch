package utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

/*Gets file size in bytes by file path*/
fun Path.sizeInBytes(): Long = fileAttributes().size()

/*Returns the time of last modification.
* If the file system implementation does not support a time stamp to indicate
* the time of last modification then this method returns an implementation
* specific default value, typically a FileTime representing the epoch (1970-01-01T00:00:00Z).
* Returns:
* a FileTime representing the time the file was last modified
*/
fun Path.lastModifiedTime(): FileTime = fileAttributes().lastModifiedTime()

/*Returns the creation time. The creation time is the time that the file was created.
*If the file system implementation does not support a time stamp to indicate the time
*when the file was created then this method returns an implementation specific default value,
*typically the last-modified-time or a FileTime representing the epoch (1970-01-01T00:00:00Z).
*Returns:
*a FileTime representing the time the file was created
* */
fun Path.lastAccessTime(): FileTime = fileAttributes().lastAccessTime()

/*Returns the creation time. The creation time is the time that the file was created.
*If the file system implementation does not support a time stamp to indicate the time
* when the file was created then this method returns an implementation specific default value,
* typically the last-modified-time or a FileTime representing the epoch (1970-01-01T00:00:00Z).
*Returns:
*a FileTime representing the time the file was created
* */
fun Path.creationTime(): FileTime = fileAttributes().creationTime()

/*Gets basic file attributes for file by path*/
fun Path.fileAttributes(): BasicFileAttributes =
    Files.readAttributes(this, BasicFileAttributes::class.java)