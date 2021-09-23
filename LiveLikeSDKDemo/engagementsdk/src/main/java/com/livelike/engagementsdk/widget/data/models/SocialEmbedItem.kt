package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName

data class SocialEmbedItem(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("oembed")
    val oEmbed: OEmbed,
    @field:SerializedName("url")
    val url: String
)

data class OEmbed(
    @field:SerializedName("author_name")
    val authorName: String,
    @field:SerializedName("author_url")
    val authorUrl: String,
    @field:SerializedName("cache_age")
    val cacheSge: String,
    @field:SerializedName("height")
    val height: Any,
    @field:SerializedName("html")
    val html: String,
    @field:SerializedName("provider_name")
    val providerName: String,
    @field:SerializedName("provider_url")
    val providerUrl: String,
    @field:SerializedName("title")
    val title: String,
    @field:SerializedName("type")
    val type: String,
    @field:SerializedName("url")
    val url: String,
    @field:SerializedName("version")
    val version: String,
    @field:SerializedName("width")
    val width: Int
)
