package com.livelike.engagementsdk.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager

internal fun Context.scanForActivity(): Activity? {
    if (this is Activity)
        return this
    else if (this is ContextWrapper)
        return this.baseContext.scanForActivity()
    return null
}

fun Context.isNetworkConnected(): Boolean {
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}
