package utils

/*format long with leading zeros*/
fun Long.format(digits: Int) = "%0${digits}d".format(this)