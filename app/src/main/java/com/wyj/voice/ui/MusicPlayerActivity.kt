package com.wyj.voice.ui

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.wyj.voice.R
import com.wyj.voice.databinding.ActivityMusicPlayerBinding
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.IPlayback
import com.wyj.voice.player.PlayList
import com.wyj.voice.player.PlayMode
import com.wyj.voice.transform.CircleTransform
import com.wyj.voice.utils.*
import com.wyj.voice.viewModel.LocalMusicViewModel
import com.wyj.voice.viewModel.MusicPlayerViewModel
import jp.wasabeef.glide.transformations.BlurTransformation
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: savedInstanceState:$savedInstanceState")
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_music_player)
        BarUtils.transparentNavBar(this)
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
        dataBinding.ivBg.background = gradientBackgroundDrawable
        musicViewModel = LocalMusicViewModel(this).apply {
            songs.observe(this@MusicPlayerActivity) {
            }
            getLocalSongs()
        }
        subscribeService()
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

    private fun subscribeService() {
        playerViewModel = MusicPlayerViewModel(this).apply {
            playbackServiceLiveData.observe(this@MusicPlayerActivity) {
                Log.d(TAG, "subscribeService: wyj player:$it")
                player = it.apply {
                    registerCallback(this@MusicPlayerActivity)
                }
                onSongUpdated(player?.getPlayingSong())
                updatePlayMode(PreferenceManager.lastPlayMode(context!!))
            }
            playModeLiveData.observe(this@MusicPlayerActivity) {
                updatePlayMode(it)
            }
            subscribe()
        }
        dataBinding.playerViewModel = playerViewModel
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
        val size = SizeUtils.dp2px(240f)
        val uri = song.album + "?param=${size}y${size}"
        Glide.with(this)
            .load(uri)
            .apply(RequestOptions()
                .placeholder(R.drawable.default_record_album)
                .transform(CircleTransform()))
            .into(dataBinding.siv)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val gradientBackgroundDrawable: GradientDrawable = GradientUtils.create(
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_gradientColor),
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_background),
            screenHeight / 2,
            0.5f,
            0.5f
        )
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .placeholder(gradientBackgroundDrawable)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val zoomBitmap = BitmapUtils.zoomImg(
                        resource,
                        displayMetrics.widthPixels,
                        displayMetrics.heightPixels
                    )
                    dataBinding.ivBg.setImageBitmap(zoomBitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            } )

        dataBinding.siv.pauseRotateAnimation()
        Log.d(TAG, "onSongUpdated: wyj isPlaying:${player?.isPlaying()}")
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
                        // 处理下滑退出，因activity已进入finish状态，如果继续更新view的状态会闪频。
                        if (it.isPlaying() && !this@MusicPlayerActivity.isFinishing) {
                            val progress: Int = (dataBinding.seekBar.max
                                    * (it.getProgress().toFloat() / getCurrentSongDuration().toFloat())).toInt()
                            updateProgressTextWithDuration(it.getProgress())
                            if (progress >= 0 && progress <= dataBinding.seekBar.max) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    dataBinding.seekBar.setProgress(progress, true)
                                } else {
                                    dataBinding.seekBar.progress = progress
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

    private fun updatePlayMode(playMode: PlayMode) {
        val res = when (playMode) {
            PlayMode.LIST -> R.drawable.ic_play_mode_list
            PlayMode.LOOP -> R.drawable.ic_play_mode_loop
            PlayMode.SHUFFLE -> R.drawable.ic_play_mode_shuffle
            PlayMode.SINGLE -> R.drawable.ic_play_mode_single
        }
        dataBinding.buttonPlayModeToggle.setImageResource(res)
    }

    private fun seekTo(duration: Int) {
        player?.seekTo(duration)
    }

    override fun onSwitchLast(last: Song) {
        onSongUpdated(last)
    }

    override fun onSwitchNext(next: Song) {
        onSongUpdated(next)
    }

    override fun onComplete(next: Song?) {
        onSongUpdated(next)
    }

    override fun onPlayStatusChanged(isPlaying: Boolean) {
        dataBinding.buttonPlayToggle.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        if (isPlaying) {
            dataBinding.siv.resumeRotateAnimation()
            scheduleTask()
        } else {
            dataBinding.siv.pauseRotateAnimation()
            timerTask?.let {
                it.cancel()
                timerTask = null
            }
        }
    }

    /**
     *  解决，滑动退出后，界面会再次绘制导致闪屏的问题。
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.bottom_silent, R.anim.bottom_out)
    }

    override fun onDestroy() {
        timerTask?.let {
            it.cancel()
            timerTask = null
        }
        timer = null
        if (player != null && playerViewModel != null) {
            playerViewModel?.unsubscribe()
            playerViewModel = null
            player?.unregisterCallback(this)
            player = null
        }
        musicViewModel?.let {
            if (it.comDisposable.isDisposed) {
                it.comDisposable.dispose()
                it.comDisposable.clear()
                musicViewModel = null
            }
        }
        super.onDestroy()
    }
}