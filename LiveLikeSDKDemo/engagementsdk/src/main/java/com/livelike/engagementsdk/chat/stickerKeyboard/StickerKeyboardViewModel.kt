package com.livelike.engagementsdk.chat.stickerKeyboard

import android.content.Context
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StickerKeyboardViewModel(private val stickerPackRepository: StickerPackRepository) {
    internal var stickerPacks =
        SubscriptionManager<List<StickerPack>>()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        ioScope.launch {
            val stickers = stickerPackRepository.getStickerPacks()
            withContext(Dispatchers.Main) {
                stickerPacks.onNext(stickers)
            }
        }
    }

    fun getFromShortcode(s: String): Sticker? {
        return stickerPackRepository.getSticker(s)
    }

    fun preload(c: Context) {
        stickerPackRepository.preloadImages(c)
    }
}
