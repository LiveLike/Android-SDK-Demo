package com.livelike.engagementsdk.widget.data.respository

import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.PREFERENCE_KEY_WIDGET_CLAIM_TOKEN
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedPreferences

class LocalPredictionWidgetVoteRepository : PredictionWidgetVoteRepository {

    override fun add(vote: PredictionWidgetVote, completion: () -> Unit) {
        val json = getSharedPreferences().getString(PREFERENCE_KEY_WIDGET_CLAIM_TOKEN, null)
        var map = gson.fromJson<MutableMap<String, String>>(json, object : TypeToken<MutableMap<String, String>>() {}.type)
        if (map == null) {
            map = mutableMapOf()
        }
        map[vote.widgetId] = vote.claimToken
        getSharedPreferences().edit().putString(PREFERENCE_KEY_WIDGET_CLAIM_TOKEN, gson.toJson(map)).apply()
    }

    override fun get(predictionWidgetID: String): String? {
        val json = getSharedPreferences().getString(PREFERENCE_KEY_WIDGET_CLAIM_TOKEN, null)
        json?.let {
            val map = gson.fromJson<MutableMap<String, String>>(json, object : TypeToken<MutableMap<String, String>>() {}.type)
            return map[predictionWidgetID]
        }
        return null
    }
}

interface PredictionWidgetVoteRepository {
    fun add(vote: PredictionWidgetVote, completion: () -> Unit)
    fun get(predictionWidgetID: String): String?
}

data class PredictionWidgetVote(val widgetId: String, val claimToken: String)
