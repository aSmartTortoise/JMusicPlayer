package com.wyj.voice.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.wyj.voice.R
import com.wyj.voice.databinding.LayoutPlayBarBinding
import com.wyj.voice.model.Song

class MusicPlayerBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener {
    private lateinit var dataBinding: LayoutPlayBarBinding
    var playCallback: PlayCallback? = null

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        dataBinding = LayoutPlayBarBinding.inflate(inflater, this, true)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        dataBinding.ivPlayToggle.setOnClickListener(this)
        dataBinding.ivSongs.setOnClickListener(this)
        dataBinding.playerBarRoot.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (playCallback == null) return
        when (v?.id) {
            R.id.iv_play_toggle -> playCallback?.onPlayToggleAction()
            R.id.iv_songs -> playCallback?.onShowSongs()
            R.id.player_bar_root -> playCallback?.onOpenMusicPlayer()
        }
    }

    fun setSong(song: Song?) {
        if (song == null) {
            dataBinding.tvSongName.text = "unknow"
            dataBinding.tvSinger.text = "unknow"
        } else {
            dataBinding.tvSongName.text = song.displayName
            dataBinding.tvSinger.text = song.artist
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        dataBinding.ivPlayToggle.setImageResource(
            if (isPlaying) R.drawable.ic_remote_view_pause else R.drawable.ic_remote_view_play)
    }

    interface PlayCallback {
        fun onPlayToggleAction()
        fun onShowSongs()
        fun onOpenMusicPlayer()
    }


}