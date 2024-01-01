package com.wyj.voice.ui.music

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
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
        window?.let {
            it.attributes = it.attributes.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT //设置宽度为铺满
                gravity = Gravity.BOTTOM
            }
        }

        adapter = SongListAdapter()
        dataBinding.rcvSongs.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SongListDialog.adapter
        }
        val current = PreferenceManager.lastPlayMode(context)
        updatePlayMode(current)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SuspiciousIndentation")
    private fun updatePlayMode(playMode: PlayMode) {
        var res = R.drawable.ic_play_mode_list_black
        var modeStr = "列表循环"
            when (playMode) {
            PlayMode.LIST -> {
                res = R.drawable.ic_play_mode_list_black
                modeStr = "列表播放"
            }
            PlayMode.LOOP -> {
                res = R.drawable.ic_play_mode_loop_black
                modeStr = "列表循环"
            }
            PlayMode.SHUFFLE -> {
                res = R.drawable.ic_play_mode_shuffle_black
                modeStr = "随机模式"
            }
            PlayMode.SINGLE -> {
                res = R.drawable.ic_play_mode_single_black
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