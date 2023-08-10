package utils

/**
 * Format double with fixed number of digits after point
 * */
fun Double.format(digits: Int) = "%.${digits}f".format(this)