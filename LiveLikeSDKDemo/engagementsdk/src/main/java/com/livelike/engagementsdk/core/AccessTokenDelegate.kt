package com.livelike.engagementsdk.core

interface AccessTokenDelegate {
    fun getAccessToken(): String?
    fun storeAccessToken(accessToken: String?)
}
