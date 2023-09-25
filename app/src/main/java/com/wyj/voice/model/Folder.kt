package com.wyj.voice.model

import java.util.Date

data class Folder(
    val id: Int, val name: String, val path: String, val numOfSongs: Int,
    val songs: MutableList<Song> = mutableListOf(), val createdAt: Date
)
