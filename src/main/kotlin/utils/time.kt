package utils

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Period formatted as HH:MM::SS.MMM starting with moment `from` till moment now
 * */
fun prettyDiffTimeFrom(from: LocalDateTime): String =
    prettyMillis(diffTime(from, LocalDateTime.now()))

/**
 * Period formatted as HH:MM::SS.MMM starting with moment `from` till moment now
 * */
fun prettyDiffTime(from: LocalDateTime, to: LocalDateTime): String =
    prettyMillis(diffTime(from, to))

/**
 * Prints milliseconds in format HH:MM:SS.MMM
 * */
fun prettyMillis(millis: Long): String {
    val hoursStr = hoursFromMillis(millis).format(2)
    val minutesStr = minutesTill60FromMillis(millis).format(2)
    val secondsStr = secondsTill60FromMillis(millis).format(2)
    val millisStr = millisTill1000FromMillis(millis).format(3)
    return "$hoursStr:$minutesStr:$secondsStr.$millisStr"
}

/**
 * Max of 2 objets LocalDateTime
 */
fun max(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a > b) a else b

/**
 * Returns formatted string with separated parts of time units, including time their names too:
 * minutes (unlimited), seconds (till 59), milliseconds (till 999)
 * */
fun longPrettyMillis(millis: Long): String =
    String.format(
        "%d minutes %d seconds %d milliseconds",
        minutesFromMillis(millis),
        secondsTill60FromMillis(millis),
        millisTill1000FromMillis(millis)
    )

/**
 * Period in milliseconds starting with moment `from` till now moment`
 * */
fun diffTime(from: LocalDateTime, to: LocalDateTime): Long {
    return Duration.between(from, to).toMillis()
}

/**
 * Takes milliseconds part from milliseconds, which referes only for milliseconds and not seconds or minutes etc
 * */
fun millisTill1000FromMillis(millis: Long) =
    millis - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))

/**
 * Takes seconds part from milliseconds, which referes only for seconds and not milliseconds or minutes etc
 * */
fun secondsTill60FromMillis(millis: Long) =
    TimeUnit.MILLISECONDS.toSeconds(millis) -
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))

/**
 * Takes minutes part from milliseconds, which referes only for minutes and not milliseconds or seconds etc
 * */
fun minutesTill60FromMillis(millis: Long) =
    TimeUnit.MILLISECONDS.toMinutes(millis) -
            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))

/**
 * Takes entire hours from milliseconds
 * */
fun hoursFromMillis(millis: Long) =
    TimeUnit.MILLISECONDS.toHours(millis)

/**
 * Takes entire minutes from milliseconds
 * */
fun minutesFromMillis(millis: Long) =
    TimeUnit.MILLISECONDS.toMinutes(millis)
