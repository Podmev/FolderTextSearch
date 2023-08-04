package bigTest

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

fun prettyDiffTime(from: LocalDateTime, to: LocalDateTime): String =
    prettyMillis(diffTime(from, to))

fun diffTime(from: LocalDateTime, to: LocalDateTime): Long {
    return Duration.between(from, to).toMillis()
}

fun prettyMillis(millis: Long): String =
    String.format(
        "%d minutes %d seconds",
        TimeUnit.MILLISECONDS.toMinutes(millis),
        TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
    )