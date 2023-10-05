package com.wyj.voice.model

data class Song(
    var id: Int = 0,
    val title: String?,
    val displayName: String?,
    val artist: String?,
    var album: String?,
    val path: String?,
    val duration: Int,
    val size: Int,
    var favorite: Boolean = false
)
