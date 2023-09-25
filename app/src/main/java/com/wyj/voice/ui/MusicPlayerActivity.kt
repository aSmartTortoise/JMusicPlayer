package com.wyj.voice.ui

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.wyj.voice.R
import com.wyj.voice.databinding.ActivityMusicPlayerBinding
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.PlayList
import com.wyj.voice.utils.AlbumUtils
import com.wyj.voice.utils.GradientUtils
import com.wyj.voice.utils.TimeUtils
import com.wyj.voice.viewmodle.LocalMusicViewModel
import com.wyj.voice.viewmodle.MusicPlayerViewModel

class MusicPlayerActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MusicPlayerActivity"
    }
    private var musicViewModel: LocalMusicViewModel? = null
    private var playerViewModel: MusicPlayerViewModel? = null
    private lateinit var dataBinding: ActivityMusicPlayerBinding

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // https://crazygui.wordpress.com/2010/09/05/high-quality-radial-gradient-in-android/
        val displayMetrics = resources.displayMetrics
        // int screenWidth = displayMetrics.widthPixels;
        val screenHeight = displayMetrics.heightPixels
        val window = window
        val gradientBackgroundDrawable: GradientDrawable = GradientUtils.create(
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_gradientColor),
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_background),
            screenHeight / 2,  // (int) Math.hypot(screenWidth / 2, screenHeight / 2),
            0.5f,
            0.5f
        )
        window.setBackgroundDrawable(gradientBackgroundDrawable)
        window.setFormat(PixelFormat.RGBA_8888)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_music_player)
        musicViewModel = LocalMusicViewModel(this).apply {
            songs.observe(this@MusicPlayerActivity) {
                for (song in it) {
                    val path = song.path
                    Log.d(TAG, "onCreate: path:$path")
                }
                val localSongs = it
                playerViewModel = MusicPlayerViewModel(this@MusicPlayerActivity).apply {
                    playbackServiceLiveData.observe(this@MusicPlayerActivity) {
                        playSong(localSongs[0])
                    }
                    subscribe()
                }
            }
            getLocalSongs()
        }

    }

    private fun playSong(song: Song) {
        val playList = PlayList(song)
        playSong(playList, 0)
    }

    private fun playSong(playList: PlayList, playIndex: Int) {
        playList.playMode = PreferenceManager.lastPlayMode(this)
        // boolean result =
        playerViewModel?.playbackServiceLiveData?.value?.play(playList, playIndex)
        val song = playList.getCurrentSong()
        onSongUpdated(song)
    }

    fun onSongUpdated( song: Song?) {
        if (song == null) {
            dataBinding.siv.cancelRotateAnimation()
            dataBinding.buttonPlayToggle.setImageResource(R.drawable.ic_play)
            dataBinding.seekBar.progress = 0
            updateProgressTextWithProgress(0)
            seekTo(0)
//            mHandler.removeCallbacks(mProgressCallback)
            return
        }

        // Step 1: Song name and artist
        dataBinding.textViewName.setText(song.displayName)
        dataBinding.textViewArtist.setText(song.artist)
        // Step 2: favorite
        dataBinding.buttonFavoriteToggle.setImageResource(
            if (song.favorite) R.drawable.ic_favorite_yes else R.drawable.ic_favorite_no)
        // Step 3: Duration
        dataBinding.tvTotalTime.setText(TimeUtils.formatDuration(song.duration))
        // Step 4: Keep these things updated
        // - Album rotation
        // - Progress(textViewProgress & seekBarProgress)
        val bitmap: Bitmap? = AlbumUtils.parseAlbum(song)
        if (bitmap == null) {
            dataBinding.siv.setImageResource(R.drawable.default_record_album)
        } else {
            dataBinding.siv.setImageBitmap(AlbumUtils.getCroppedBitmap(bitmap))
        }
        dataBinding.siv.pauseRotateAnimation()
//        mHandler.removeCallbacks(mProgressCallback)
        if (playerViewModel?.playbackServiceLiveData?.value?.isPlaying() == true) {
            dataBinding.siv.startRotateAnimation()
//            mHandler.post(mProgressCallback)
            dataBinding.buttonPlayToggle.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun updateProgressTextWithProgress(progress: Int) {
        val targetDuration: Int = getDuration(progress)
        dataBinding.tvProgress.text = TimeUtils.formatDuration(targetDuration)
    }

    private fun getDuration(progress: Int): Int {
        return (getCurrentSongDuration() * (progress.toFloat() / dataBinding.seekBar.max)).toInt()
    }

    private fun getCurrentSongDuration(): Int {
        val currentSong: Song? = playerViewModel?.playbackServiceLiveData?.value?.getPlayingSong()
        var duration = 0
        if (currentSong != null) {
            duration = currentSong.duration
        }
        return duration
    }

    private fun seekTo(duration: Int) {
        playerViewModel?.playbackServiceLiveData?.value?.seekTo(duration)
    }

    override fun onDestroy() {
        super.onDestroy()
        musicViewModel?.let {
            if (it.comDisposable.isDisposed) {
                it.comDisposable.dispose()
                it.comDisposable.clear()
                musicViewModel = null
            }
        }
    }


}