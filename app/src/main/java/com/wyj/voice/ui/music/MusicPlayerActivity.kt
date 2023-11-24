package com.wyj.voice.ui.music

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*


class MusicPlayerActivity : AppCompatActivity(), IPlayback.Callback {
    companion object {
        const val TAG = "MusicPlayerActivity"
    }
    private var musicViewModel: LocalMusicViewModel? = null
    private var playerViewModel: MusicPlayerViewModel? = null
    private lateinit var dataBinding: ActivityMusicPlayerBinding
    private var player: IPlayback? = null
    private var playProgressJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: savedInstanceState:$savedInstanceState")
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_music_player)
        BarUtils.transparentNavBar(this)
        dataBinding.titleBar.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)
        // https://crazygui.wordpress.com/2010/09/05/high-quality-radial-gradient-in-android/
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val gradientBackgroundDrawable: GradientDrawable = GradientUtils.create(
            ContextCompat.getColor(this, R.color.color_primary_pressed),
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
                cancelPlayProgressJob()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekTo(getDuration(seekBar.progress))
                if (player?.isPlaying() == true) {
                    launchUpdateProgressJob()
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
            cancelPlayProgressJob()
            return
        }

        // Step 1: Song name and artist
        dataBinding.textViewName.text = song.displayName
        dataBinding.textViewArtist.text = song.artist
        // Step 2: Duration
        dataBinding.tvTotalTime.text = TimeUtils.formatDuration(song.duration)
        // Step 3: Keep these things updated
        val size = SizeUtils.dp2px(240f)
        Glide.with(this)
            .load(song.album)
            .apply(RequestOptions()
                .placeholder(R.drawable.default_record_album)
                .transform(CircleTransform())
                .override(size, size))
            .into(dataBinding.siv)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val gradientBackgroundDrawable: GradientDrawable = GradientUtils.create(
            ContextCompat.getColor(this, R.color.color_primary_pressed),
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_background),
            screenHeight / 2,
            0.5f,
            0.5f
        )
        Glide.with(this)
            .asBitmap()
            .load(song.album)
            .placeholder(gradientBackgroundDrawable)
            .apply(
                RequestOptions()
                    .override(size, size)
                    .transform(BlurTransformation(20, 3))
            )
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val zoomBitmap = BitmapUtils.zoomImg(
                        resource,
                        displayMetrics.heightPixels,
                        displayMetrics.heightPixels
                    )
                    dataBinding.ivBg.setImageBitmap(zoomBitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            } )

        dataBinding.siv.pauseRotateAnimation()
        Log.d(TAG, "onSongUpdated: wyj isPlaying:${player?.isPlaying()}")
        // - Album rotation
        if (player?.isPlaying() == true) {
            dataBinding.siv.startRotateAnimation()
            launchUpdateProgressJob()
            dataBinding.buttonPlayToggle.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun cancelPlayProgressJob() {
        playProgressJob?.let {
            it.cancel()
            playProgressJob = null
        }
    }

    private fun launchUpdateProgressJob() {
        cancelPlayProgressJob()
        playProgressJob = flow {
            repeat(Int.MAX_VALUE) {
                Log.d(TAG, "scheduleTask: isActive:${currentCoroutineContext()[Job]?.isActive}")
                player?.let {
                    if (it.isPlaying()) {
                        emit(it.getProgress())
                        delay(1000)
                    }
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .onEach {
                Log.d(TAG, "scheduleTask: each progress:$it")
                if (!this@MusicPlayerActivity.isFinishing) {
                    updateProgressTextWithDuration(it)
                }
            }
            .map {
                (dataBinding.seekBar.max * it.toFloat() / getCurrentSongDuration().toFloat()).toInt()
            }
            .onEach {
                if (!this@MusicPlayerActivity.isFinishing) {
                    if (it >= 0 && it <= dataBinding.seekBar.max) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            dataBinding.seekBar.setProgress(it, true)
                        } else {
                            dataBinding.seekBar.progress = it
                        }
                    }
                }
            }
            .flowOn(Dispatchers.Main.immediate)
            .launchIn(lifecycleScope)
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
            launchUpdateProgressJob()
        } else {
            dataBinding.siv.pauseRotateAnimation()
            cancelPlayProgressJob()
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
        cancelPlayProgressJob()
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