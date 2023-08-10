package utils

/**
 * Formats string for long value of bytes (memory).
 * https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
 * */
fun prettyBytes(v: Long): String {
    if (v < 1024) return "$v B"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(v)) / 10
    return String.format("%.1f %sB", v.toDouble() / (1L shl z * 10), " KMGTPE"[z])
}