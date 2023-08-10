package utils

/**
 * Format long with fixed length using leading zeros
 * */
fun Long.format(digits: Int) = "%0${digits}d".format(this)