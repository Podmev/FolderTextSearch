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

fun prettyMillis(millis: Long): String =
    String.format(
        "%d minutes %d seconds %d milliseconds",
        TimeUnit.MILLISECONDS.toMinutes(millis),
        TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
        millis - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))
    )