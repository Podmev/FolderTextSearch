package utils

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

fun prettyDiffTimeFrom(from: LocalDateTime): String =
    prettyMillis(diffTime(from, LocalDateTime.now()))

fun prettyDiffTime(from: LocalDateTime, to: LocalDateTime): String =
    prettyMillis(diffTime(from, to))

fun diffTime(from: LocalDateTime, to: LocalDateTime): Long {
    return Duration.between(from, to).toMillis()
}

fun millisTill1000FromMillis(millis: Long) =
    millis - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))

fun secondsTill60FromMillis(millis: Long) =
    TimeUnit.MILLISECONDS.toSeconds(millis) -
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))

fun minutesTill60FromMillis(millis: Long) =
    TimeUnit.MILLISECONDS.toMinutes(millis) -
            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))

fun hoursFromMillis(millis: Long) =
    TimeUnit.MILLISECONDS.toHours(millis)

fun prettyMillis(millis: Long): String {
    val hoursStr = hoursFromMillis(millis).format(2)
    val minutesStr = minutesTill60FromMillis(millis).format(2)
    val secondsStr = secondsTill60FromMillis(millis).format(2)
    val millisStr = millisTill1000FromMillis(millis).format(3)
    return "$hoursStr:$minutesStr:$secondsStr.$millisStr"
}

fun longPrettyMillis(millis: Long): String =
    String.format(
        "%d minutes %d seconds %d milliseconds",
        TimeUnit.MILLISECONDS.toMinutes(millis),
        TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
        millis - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))
    )