package com.livelike.engagementsdk

import android.graphics.Typeface
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.widget.BaseTheme
import com.livelike.engagementsdk.widget.WidgetsTheme

class LiveLikeEngagementTheme internal constructor(
    val chat: Map<String, Any?>? = null,
    val version: Double,
    val widgets: WidgetsTheme
) : BaseTheme() {

    var fontFamilyProvider: FontFamilyProvider? = null

    override fun validate(): String? {
        return widgets.validate()
    }

    companion object {
        @JvmStatic
        fun instanceFrom(
            themeJson: JsonObject,
            fontFamilyProvider: FontFamilyProvider? = null
        ): Result<LiveLikeEngagementTheme> {
            return try {
                val data = gson.fromJson(
                    themeJson,
                    LiveLikeEngagementTheme::class.java
                )
                val errorString = data.validate()
                if (errorString == null) {
                    data.fontFamilyProvider = fontFamilyProvider
                    Result.Success(data)
                } else {
                    Result.Error(RuntimeException(errorString))
                }
            } catch (ex: JsonParseException) {
                Result.Error(ex)
            }
        }
    }
}

interface FontFamilyProvider {

    /**
     * if no font family associated to name then return null.
     **/
    fun getTypeFace(fontFamilyName: String): Typeface?
}
