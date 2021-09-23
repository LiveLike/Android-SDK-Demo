package com.livelike.engagementsdk.chat.stickerKeyboard

import android.content.Context
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class StickerPackRepository(val programId: String, private val endpoint: String) {
    private var stickerPackList: List<StickerPack>? = null

    suspend fun getStickerPacks(): List<StickerPack> {
        return withContext(Dispatchers.IO) {
            if (stickerPackList == null) {
                stickerPackList = try {
                    val response = URL(endpoint).readText()
                    val stickerRes = Gson().fromJson(response, StickerPackResults::class.java)
                    stickerRes.results.apply {
                        // Set the Current Program Id to all the stickers.
                        forEach { it.stickers.forEach { st -> st.programId = programId } }
                    }
                } catch (e: Exception) {
                    listOf()
                }
            }
            return@withContext stickerPackList as List<StickerPack>
        }
    }

    fun preloadImages(context: Context) {
        stickerPackList?.forEach {
            Glide.with(context).load(it.file).preload()
            it.stickers.forEach { sticker ->
                Glide.with(context).load(sticker.file).preload()
            }
        }
    }

    // When in the input the user type :shortcode: a regex captures it and this fun is used to find the corresponding sticker if it exists.
    fun getSticker(shortcode: String): Sticker? {
        return stickerPackList?.map { it.stickers }?.flatten()?.find { it.shortcode == shortcode }
    }
}
