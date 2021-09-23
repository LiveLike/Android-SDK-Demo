package com.livelike.engagementsdk.core.utils

import android.util.Log
import java.io.IOException

/** The different verbosity types */
enum class LogLevel(
    val code: Int,
    val logger: (String, String) -> Int,
    val exceptionLogger: (String, String, Throwable) -> Int
) {
    /** Highly detailed level of logging, best used when trying to understand the working of a specific section/feature of the Engagement SDK. */
    Verbose(Log.VERBOSE, Log::v, Log::v),
    /** Information that is diagnostically helpful to integrators and Engagement SDK developers. */
    Debug(Log.DEBUG, Log::d, Log::d),
    /** Information that is always useful to have, but not vital. */
    Info(Log.INFO, Log::i, Log::i),
    /** Information related to events that could potentially cause oddities, but the Engagement SDK will continue working as expected. */
    Warn(Log.WARN, Log::w, Log::w),
    /** An error occurred that is fatal to a specific operation/component, but not the overall Engagement SDK. */
    Error(Log.ERROR, Log::e, Log::e),
    /** No logging enabled. */
    None(Log.ASSERT + 1, { _, _ -> 0 }, { _, _, _ -> 0 })
}

/** The lowest (most granular) log level to log */
var minimumLogLevel: LogLevel =
    LogLevel.Verbose

internal inline fun <reified T> T.logVerbose(message: () -> Any?) = log(LogLevel.Verbose, message)
internal inline fun <reified T> T.logDebug(message: () -> Any?) = log(LogLevel.Debug, message)
internal inline fun <reified T> T.logInfo(message: () -> Any?) = log(LogLevel.Info, message)
internal inline fun <reified T> T.logWarn(message: () -> Any?) = log(LogLevel.Warn, message)
internal inline fun <reified T> T.logError(message: () -> Any?) = log(LogLevel.Error, message)

private var handler: ((String) -> Unit)? = null

/**
 * Add a log handler to intercept the logs.
 * This can be helpful for debugging.
 *
 * @param logHandler Method processing the log string received.
 */
fun registerLogsHandler(logHandler: (String) -> Unit) {
    handler = logHandler
}

/**
 * call it to tear down the registered logs handler
 */

fun unregisterLogsHandler() {
    handler = null
}

internal inline fun <reified T> T.log(level: LogLevel, message: () -> Any?) {
    if (level >= minimumLogLevel) {
        message().let {
            val tag = T::class.java.canonicalName ?: "com.livelike"
            when (it) {
                is Throwable -> level.exceptionLogger(tag, it.message ?: "", it)
                is Unit -> Unit
                null -> Unit
                else -> level.logger(tag, it.toString())
            }
        }
        message().let {
            handler?.invoke(it.toString())
        }
    }
}

@Suppress("unused")
private class LoggerSample {
    data class Fruit(val name: String, val qty: Int)

    val threeApples: Fruit? =
        Fruit("Apple", 3)

    fun basicLogging() {
        minimumLogLevel =
            LogLevel.Verbose

        logVerbose { "Just an informative message that no-one really cares about" }

        logDebug { "This might be interesting" }

        logInfo { threeApples ?: "no apples" }

        logWarn { RuntimeException("This wasn't supposed to happen but it doesn't matter") }

        logError { IOException("Could not connect to nobody nowhere") }
    }
}
