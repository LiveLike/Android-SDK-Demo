package com.livelike.engagementsdk.core.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.livelike.engagementsdk.parseISODateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.lang.reflect.Type

internal fun JsonObject.extractStringOrEmpty(propertyName: String): String {
    return if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asString else ""
}

internal fun JsonObject.extractBoolean(propertyName: String): Boolean {
    return if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asBoolean else false
}

internal fun JsonObject.extractLong(propertyName: String, default: Long = 0): Long {
    var returnVal = default
    try {
        returnVal =
            if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asLong else default
    } catch (e: NumberFormatException) {
        logError { "Failed to extractLong: $e" }
    }
    return returnVal
}

internal val gson = GsonBuilder()
    .registerTypeAdapter(
        ZonedDateTime::class.java,
        DateDeserializer()
    )
    .registerTypeAdapter(
        ZonedDateTime::class.java,
        DateSerializer()
    )
    .create()!!

internal val isoUTCDateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

internal class DateDeserializer : JsonDeserializer<ZonedDateTime> {

    override fun deserialize(
        element: JsonElement,
        arg1: Type,
        arg2: JsonDeserializationContext
    ): ZonedDateTime? {
        val date = element.asString
        return date.parseISODateTime()
    }
}

internal class DateSerializer : JsonSerializer<ZonedDateTime> {
    override fun serialize(
        src: ZonedDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val obj = JsonObject()
        obj.addProperty("program_date_time", isoUTCDateTimeFormatter.format(src).toString())
        return obj.get("program_date_time")
    }
}
