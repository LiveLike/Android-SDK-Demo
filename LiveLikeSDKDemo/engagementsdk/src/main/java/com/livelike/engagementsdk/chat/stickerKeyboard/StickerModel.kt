package com.livelike.engagementsdk.chat.stickerKeyboard

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Sticker(val file: String, val shortcode: String, var programId: String = "") : Parcelable

data class StickerPack(val name: String, val file: String, val stickers: List<Sticker>)
data class StickerPackResults(val results: List<StickerPack>)
