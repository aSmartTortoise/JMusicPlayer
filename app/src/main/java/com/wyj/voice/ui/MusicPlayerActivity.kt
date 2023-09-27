package com.wyj.voice.ui

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.wyj.voice.R
import com.wyj.voice.databinding.ActivityMusicPlayerBinding
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.IPlayback
import com.wyj.voice.player.PlayList
import com.wyj.voice.utils.AlbumUtils
import com.wyj.voice.utils.BarUtils
import com.wyj.voice.utils.GradientUtils
import com.wyj.voice.utils.TimeUtils
import com.wyj.voice.viewmodle.LocalMusicViewModel
import com.wyj.voice.viewmodle.MusicPlayerViewModel
import java.util.*


class MusicPlayerActivity : AppCompatActivity(), IPlayback.Callback {
    companion object {
        const val TAG = "MusicPlayerActivity"
    }
    private var musicViewModel: LocalMusicViewModel? = null
    private var playerViewModel: MusicPlayerViewModel? = null
    private lateinit var dataBinding: ActivityMusicPlayerBinding
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var player: IPlayback? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // https://crazygui.wordpress.com/2010/09/05/high-quality-radial-gradient-in-android/
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val gradientBackgroundDrawable: GradientDrawable = GradientUtils.create(
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_gradientColor),
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_background),
            screenHeight / 2,  // (int) Math.hypot(screenWidth / 2, screenHeight / 2),
            0.5f,
            0.5f
        )
        window.setBackgroundDrawable(gradientBackgroundDrawable)
        window.setFormat(PixelFormat.RGBA_8888)
        BarUtils.transparentNavBar(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: savedInstanceState:$savedInstanceState")
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_music_player)
        musicViewModel = LocalMusicViewModel(this).apply {
            songs.observe(this@MusicPlayerActivity) {
                subscribeService(it)
            }
            getLocalSongs()
        }
        dataBinding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateProgressTextWithProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                timerTask?.let {
                    it.cancel()
                    timerTask = null
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekTo(getDuration(seekBar.progress))
                if (player?.isPlaying() == true) {
                    scheduleTask()
                }
            }
        })
    }

    private fun subscribeService(localSongs: List<Song>) {
        if (player == null) {
            playerViewModel = MusicPlayerViewModel(this).apply {
                playbackServiceLiveData.observe(this@MusicPlayerActivity) {
                    player = it.apply {
                        registerCallback(this@MusicPlayerActivity)
                    }
                    playSong(localSongs)
                }
                subscribe()
            }
        }
    }

    private fun playSong(song: Song) {
        val playList = PlayList(song)
        playSong(playList, 0)
    }

    private fun playSong(songs: List<Song>) {
        val playList = PlayList()
        playList.addSong(songs, 0)
        playSong(playList, 0)
    }

    private fun playSong(playList: PlayList, playIndex: Int) {
        playList.playMode = PreferenceManager.lastPlayMode(this)
        player?.play(playList, playIndex)
        val song = playList.getCurrentSong()
        onSongUpdated(song)
    }

    private fun onSongUpdated(song: Song?) {
        if (song == null) {
            dataBinding.siv.cancelRotateAnimation()
            dataBinding.buttonPlayToggle.setImageResource(R.drawable.ic_play)
            dataBinding.seekBar.progress = 0
            updateProgressTextWithProgress(0)
            seekTo(0)
            timerTask?.let {
                it.cancel()
                timerTask = null
            }
            return
        }

        // Step 1: Song name and artist
        dataBinding.textViewName.text = song.displayName
        dataBinding.textViewArtist.text = song.artist
        // Step 2: favorite
        dataBinding.buttonFavoriteToggle.setImageResource(
            if (song.favorite) R.drawable.ic_favorite_yes else R.drawable.ic_favorite_no)
        // Step 3: Duration
        dataBinding.tvTotalTime.text = TimeUtils.formatDuration(song.duration)
        // Step 4: Keep these things updated
        // - Album rotation
        val bitmap: Bitmap? = AlbumUtils.parseAlbum(song)
        if (bitmap == null) {
            dataBinding.siv.setImageResource(R.drawable.default_record_album)
        } else {
            dataBinding.siv.setImageBitmap(AlbumUtils.getCroppedBitmap(bitmap))
        }
        dataBinding.siv.pauseRotateAnimation()
        if (player?.isPlaying() == true) {
            dataBinding.siv.startRotateAnimation()
            scheduleTask()
            dataBinding.buttonPlayToggle.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun scheduleTask() {
        if (timer == null) {
            timer = Timer()
        }
        timerTask?.let {
            it.cancel()
            timerTask = null
        }
        timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    player?.let {
                        if (it.isPlaying()) {
                            val progress: Int = (dataBinding.seekBar.max
                                    * (it.getProgress().toFloat() / getCurrentSongDuration().toFloat())).toInt()
                            updateProgressTextWithDuration(it.getProgress())
                            if (progress >= 0 && progress <= dataBinding.seekBar.max) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    dataBinding.seekBar.setProgress(progress, true)
                                } else {
                                    dataBinding.seekBar.setProgress(progress)
                                }
                            }
                        }
                    }
                }
            }
        }
        timer?.schedule(timerTask, 0, 50)
    }


    private fun updateProgressTextWithProgress(progress: Int) {
        val targetDuration: Int = getDuration(progress)
        dataBinding.tvProgress.text = TimeUtils.formatDuration(targetDuration)
    }

    private fun updateProgressTextWithDuration(duration: Int) {
        dataBinding.tvProgress.text = TimeUtils.formatDuration(duration)
    }

    private fun getDuration(progress: Int): Int {
        return (getCurrentSongDuration() * (progress.toFloat() / dataBinding.seekBar.max)).toInt()
    }

    private fun getCurrentSongDuration(): Int {
        val currentSong: Song? = player?.getPlayingSong()
        var duration = 0
        if (currentSong != null) {
            duration = currentSong.duration
        }
        return duration
    }

    private fun seekTo(duration: Int) {
        player?.seekTo(duration)
    }

    override fun onSwitchLast(last: Song) {
    }

    override fun onSwitchNext(next: Song) {
    }

    override fun onComplete(next: Song?) {
        onSongUpdated(next)
    }

    override fun onPlayStatusChanged(isPlaying: Boolean) {

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: wyj")
        musicViewModel?.let {
            if (it.comDisposable.isDisposed) {
                it.comDisposable.dispose()
                it.comDisposable.clear()
                musicViewModel = null
            }
        }

        if (player != null && playerViewModel != null) {
            playerViewModel?.unsubscribe()
            playerViewModel = null
            player = null
        }

        timerTask?.let {
            it.cancel()
            timerTask = null
        }
        timer = null

    }
}