package com.example.musicplayer.data.api

import com.google.gson.annotations.SerializedName

data class StreamUrlResponse(
    @SerializedName("stream_url")
    val streamUrl: String
)