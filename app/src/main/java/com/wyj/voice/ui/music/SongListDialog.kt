package com.wyj.voice.ui.music

import android.R.color
import android.R.drawable
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wyj.voice.R
import com.wyj.voice.databinding.DialogSongListBinding
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.PlayMode


class SongListDialog @JvmOverloads constructor(context: Context, theme: Int = 0) :
    BottomSheetDialog(context, theme) {
    private lateinit var adapter: SongListAdapter
    private lateinit var dataBinding: DialogSongListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = DialogSongListBinding.inflate(LayoutInflater.from(context))
        setContentView(dataBinding.root)

        adapter = SongListAdapter()
        dataBinding.rcvSongs.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SongListDialog.adapter
        }
        val current = PreferenceManager.lastPlayMode(context)
        updatePlayMode(current)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updatePlayMode(playMode: PlayMode) {
        var res = R.drawable.ic_play_mode_list
        var modeStr = "列表循环"
            when (playMode) {
            PlayMode.LIST -> {
                res = R.drawable.ic_play_mode_list
                modeStr = "列表播放"
            }
            PlayMode.LOOP -> {
                res = R.drawable.ic_play_mode_loop
                modeStr = "列表循环"
            }
            PlayMode.SHUFFLE -> {
                res = R.drawable.ic_play_mode_shuffle
                modeStr = "随机模式"
            }
            PlayMode.SINGLE -> {
                res = R.drawable.ic_play_mode_single
                modeStr = "单曲播放"
            }
        }
        dataBinding.tvPlayMode.text = modeStr
        val drawableLeft = context.resources.getDrawable(res, null)
        dataBinding.tvPlayMode.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null)
    }

    fun setSongs(songs: List<Song>) {
        adapter.setData(songs)
    }
}