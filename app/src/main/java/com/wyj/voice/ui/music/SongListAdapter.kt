package com.wyj.voice.ui.music

import android.text.TextUtils
import com.jie.databinding.base.BaseAdapter
import com.wyj.voice.databinding.ItemViewSongBinding
import com.wyj.voice.model.Song
import com.wyj.voice.utils.SpanUtils

class SongListAdapter :
    BaseAdapter<Song, ItemViewSongBinding>() {
    override fun ItemViewSongBinding.onBindViewHolder(song: Song, position: Int) {
        if (TextUtils.isEmpty(song.displayName)) {
            song.displayName = "unKnow"
        }
        if (TextUtils.isEmpty(song.artist)) {
            song.artist = "unKnow"
        }
        SpanUtils.with(tvSongName)
            .append(song.displayName!!)
            .setFontSize(14, true)
            .append("  ${song.artist!!}")
            .setFontSize(12, true)
            .create()
    }
}