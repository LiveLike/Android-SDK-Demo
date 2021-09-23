package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName

data class Badge(
    @SerializedName("id")
    val id: String,
    @SerializedName("file")
    val imageFile: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("level")
    val level: Int,
    @SerializedName("mimetype")
    val mimetype: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("points")
    val points: Int
) : Comparable<Badge> {

    override fun compareTo(other: Badge): Int {
        return level.compareTo(other.level)
    }
}
