package com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs

import android.content.Context
import android.content.SharedPreferences

private const val PREFERENCE_KEY_SESSION_ID = "SessionId"
private const val PREFERENCE_KEY_ACCESS_TOKEN = "AccessToken"
private const val PREFERENCE_KEY_NICKNAME = "Username"
private const val PREFERENCE_KEY_USER_PIC = "Userpic"
internal const val PREFERENCE_KEY_WIDGET_CLAIM_TOKEN = "claim_token"
private const val BLOCKED_USERS = "blocked-users"
private var mAppContext: Context? = null

internal fun initLiveLikeSharedPrefs(appContext: Context) {
    mAppContext = appContext
}

internal fun getSharedPreferences(): SharedPreferences {
    val packageName = mAppContext?.packageName ?: ""
    return mAppContext!!.getSharedPreferences("$packageName-livelike-sdk", Context.MODE_PRIVATE)
}

internal fun setSharedAccessToken(token: String) {
    getSharedPreferences()
        .edit().putString(PREFERENCE_KEY_ACCESS_TOKEN, token).apply()
}

internal fun getSharedAccessToken(): String? {
    return getSharedPreferences().getString(PREFERENCE_KEY_ACCESS_TOKEN, null)
}

internal fun getSessionId(): String {
    return getSharedPreferences()
        .getString(PREFERENCE_KEY_SESSION_ID, "") ?: ""
}

internal fun setNickname(nickname: String) {
    val editor = getSharedPreferences()
        .edit()
    editor.putString(PREFERENCE_KEY_NICKNAME, nickname).apply()
}

internal fun getNickename(): String {
    return getSharedPreferences()
        .getString(PREFERENCE_KEY_NICKNAME, "") ?: ""
}

internal fun blockUser(userId: String) {
    val editor = getSharedPreferences()
        .edit()
    val currentList = getSharedPreferences()
        .getString(BLOCKED_USERS, "") ?: ""
    if (!currentList.contains(userId)) {
        editor.putString(BLOCKED_USERS, "$currentList,$userId").apply()
    }
}

internal fun getBlockedUsers(): List<String> {
    val currentList = getSharedPreferences()
        .getString(BLOCKED_USERS, "") ?: ""
    return currentList.split(",")
}
