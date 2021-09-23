package com.livelike.engagementsdk.chat.chatreaction

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class Reaction(
    val emojis: String,
    val name: String,
    var file: String,
    val id: String
) : Parcelable

internal data class ReactionPack(val name: String, val file: String, val emojis: List<Reaction>)
internal data class ReactionPackResults(val results: List<ReactionPack>)
