package com.wyj.voice.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wyj.voice.R
import com.wyj.voice.databinding.LayoutPlayBarBinding
import com.wyj.voice.model.Song
import com.wyj.voice.transform.CircleTransform
import com.wyj.voice.utils.AlbumUtils

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
            dataBinding.sivAlbum.setImageResource(R.drawable.default_record_album)

        } else {
            dataBinding.tvSongName.text = song.displayName
            dataBinding.tvSinger.text = song.artist
            Glide.with(this)
                .load(song.album)
                .apply(
                    RequestOptions()
                    .placeholder(R.drawable.default_record_album)
                    .transform(CircleTransform()))
                .into(dataBinding.sivAlbum)
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        dataBinding.sivAlbum.pauseRotateAnimation()
        if (isPlaying) {
            dataBinding.ivPlayToggle.setImageResource(R.drawable.ic_remote_view_pause)
            dataBinding.sivAlbum.startRotateAnimation()
        } else {
            dataBinding.ivPlayToggle.setImageResource(R.drawable.ic_remote_view_play)
        }
    }

    interface PlayCallback {
        fun onPlayToggleAction()
        fun onShowSongs()
        fun onOpenMusicPlayer()
    }


}