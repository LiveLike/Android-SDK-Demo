package com.livelike.engagementsdk.sponsorship

import com.google.gson.annotations.SerializedName

data class SponsorModel(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("name")
    val name: String,
    @field:SerializedName("client_id")
    val clientId: String,
    @field:SerializedName("logo_url")
    val logoUrl: String
)
