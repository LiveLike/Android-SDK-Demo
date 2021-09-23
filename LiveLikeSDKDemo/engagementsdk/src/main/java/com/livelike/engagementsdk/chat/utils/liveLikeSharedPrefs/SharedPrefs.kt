package com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs

import com.livelike.engagementsdk.chat.stickerKeyboard.Sticker
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerPack
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedPreferences

private const val RECENT_STICKERS = "recent-stickers"
private const val RECENT_STICKERS_DELIMITER = "~~~~"

internal fun addRecentSticker(sticker: Sticker) {
    val editor = getSharedPreferences()
        .edit()
    val stickerSet: MutableSet<String> = HashSet(
        getSharedPreferences()
            .getStringSet(RECENT_STICKERS + sticker.programId, setOf()) ?: setOf()
    ).toMutableSet() // The data must be copied to a new array, see doc https://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String,%20java.util.Set%3Cjava.lang.String%3E)
    stickerSet.add(sticker.file + RECENT_STICKERS_DELIMITER + sticker.shortcode)
    editor.putStringSet(RECENT_STICKERS + sticker.programId, stickerSet)?.apply()
}

internal fun filterRecentStickers(programId: String, stickerPacks: List<StickerPack>) {
    val stickerSet: Set<String> =
        getSharedPreferences()
            .getStringSet(RECENT_STICKERS + programId, setOf()) ?: setOf()

    val totalStickerSet: Set<String> = when (stickerPacks.isEmpty()) {
        true -> setOf()
        else -> stickerPacks.map { stickerPack -> stickerPack.stickers.map { sticker -> sticker.file + RECENT_STICKERS_DELIMITER + sticker.shortcode } }
            .reduceRight { list, list2 -> list.plus(list2) }.toSet()
    }
    val updatedStickerSet = stickerSet.filter { totalStickerSet.contains(it) }.toMutableSet()
    val editor = getSharedPreferences()
        .edit()
    editor.putStringSet(RECENT_STICKERS + programId, updatedStickerSet)?.apply()
}

internal fun getRecentStickers(programId: String): List<Sticker> {
    val stickerSet: Set<String> =
        getSharedPreferences()
            .getStringSet(RECENT_STICKERS + programId, setOf()) ?: setOf()
    return stickerSet.map {
        Sticker(
            it.split(RECENT_STICKERS_DELIMITER)[0],
            it.split(
                RECENT_STICKERS_DELIMITER
            )[1]
        )
    }
}

internal fun addPublishedMessage(channel: String, messageId: String) {
    val msgList = getPublishedMessages(channel)
    msgList.add(messageId)
    val editor = getSharedPreferences()
        .edit()
    editor.putStringSet("$channel-published", msgList).apply()
}

internal fun flushPublishedMessage(vararg channels: String) {
    val editor = getSharedPreferences()
        .edit()
    channels.forEach { channel ->
        editor.remove("$channel-published")
    }
    editor.apply()
}

internal fun getPublishedMessages(channel: String): MutableSet<String> {
    return getSharedPreferences()
        .getStringSet("$channel-published", mutableSetOf())
        ?: mutableSetOf()
}
