package com.livelike.engagementsdk

import com.livelike.engagementsdk.core.utils.logError
import org.threeten.bp.DateTimeException
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
import java.util.regex.Pattern

/**
 * A fixed moment in time with a specified baseline and precision.
 *
 * @property timeSinceEpochInMs Number of milliseconds that have elapsed since 00:00:00, 1 January 1970 UTC
 */
class EpochTime(val timeSinceEpochInMs: Long) : Comparable<EpochTime> {
    /**
     * Compare two EpochTime objects
     */
    override fun compareTo(other: EpochTime): Int {
        return timeSinceEpochInMs.compareTo(other.timeSinceEpochInMs)
    }

    /**
     * Subtracts two EpochTime objects
     */
    operator fun minus(et: EpochTime) =
        EpochTime(timeSinceEpochInMs - et.timeSinceEpochInMs)

    /**
     * Subtracts an EpochTime object with a timeStamp in Ms
     */
    operator fun minus(timeStampMs: Long) =
        EpochTime(timeSinceEpochInMs - timeStampMs)

    /**
     * Adds two EpochTime objects
     */
    operator fun plus(et: EpochTime) =
        EpochTime(timeSinceEpochInMs + et.timeSinceEpochInMs)

    /**
     * Adds an EpochTime objects with a timeStamp in Ms
     */
    operator fun plus(timeStampMs: Long) =
        EpochTime(timeSinceEpochInMs + timeStampMs)
}

@Deprecated("Use {@link #parseISODateTime()} instead")
internal fun String.parseISO8601(): ZonedDateTime? {
    return try {
        if (isEmpty()) null else ZonedDateTime.parse(
            this,
            DateTimeFormatter.ISO_DATE_TIME
        )
    } catch (exception: org.threeten.bp.format.DateTimeParseException) {
        logError { "Failed to parse Date due to : $exception" }
        null
    }
}

private val ISO_DATE_TIME_PATTERN = Pattern.compile(
    "(\\d\\d\\d\\d)\\-(\\d\\d)\\-(\\d\\d)[Tt]" +
        "(\\d\\d):(\\d\\d):(\\d\\d)([\\.,](\\d+))?" +
        "([Zz]|((\\+|\\-)(\\d?\\d):?(\\d\\d)))?"
)

/**
 * @param value The attribute value to decode.
 * @return The parsed ZonedDateTime.
 */
internal fun String.parseISODateTime(): ZonedDateTime? {

    val matcher = ISO_DATE_TIME_PATTERN.matcher(this)
    if (!matcher.matches()) {
        logError { "Invalid date/time format: $this" }
        return null
    }

    var timezoneShift: Int
    if (matcher.group(9) == null) {
        // No time zone specified.
        timezoneShift = 0
    } else if (matcher.group(9).equals("Z", false)) {
        timezoneShift = 0
    } else {
        timezoneShift =
            Integer.parseInt(matcher.group(12)) * 60 + Integer.parseInt(matcher.group(13))
        if ("-" == matcher.group(11)) {
            timezoneShift *= -1
        }
    }

    var nanoSeconds = 0
    if (matcher.group(8) != null && matcher.group(8).isNotEmpty()) {
        val bd = BigDecimal("0." + matcher.group(8))
        // we care only for milliseconds, so movePointRight(3)
        nanoSeconds = bd.movePointRight(9).toInt()
    }

    return try {
        ZonedDateTime.of(
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)),
            Integer.parseInt(matcher.group(4)),
            Integer.parseInt(matcher.group(5)),
            Integer.parseInt(matcher.group(6)),
            nanoSeconds,
            ZoneId.ofOffset("GMT", ZoneOffset.ofTotalSeconds(timezoneShift * 60))
        )
    } catch (e: DateTimeException) {
        logError { e.message }
        null
    }
}

/** Formats in 2019-09-17T17:31:08.914+05:30[Asia/Kolkata] */
internal fun ZonedDateTime.formatIso8601(): String {
    return DateTimeFormatter.ISO_DATE_TIME.format(this)
}

/** Formats in 2019-09-17T10:30:56 */
internal fun ZonedDateTime.formatIsoZoned8601(): String {
    return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(this)
}
