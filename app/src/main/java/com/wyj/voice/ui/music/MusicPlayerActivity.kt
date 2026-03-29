package com.wyj.voice.ui.music

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.wyj.voice.player.PlayMode
import com.wyj.voice.transform.CircleTransform
import com.wyj.voice.utils.*
import com.wyj.voice.viewModel.LocalMusicViewModel
import com.wyj.voice.viewModel.MusicPlayerViewModel
import androidx.lifecycle.ViewModelProvider
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*


class MusicPlayerActivity : AppCompatActivity(), IPlayback.Callback {
    companion object {
        const val TAG = "MusicPlayerActivity"
        const val REQ_NOTIFICATION_CODE = 2
    }
    private var musicViewModel: LocalMusicViewModel? = null
    private lateinit var playerViewModel: MusicPlayerViewModel
    private lateinit var dataBinding: ActivityMusicPlayerBinding
    private var playProgressJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        initListener()
    }

    private fun initListener() {
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
                if (playerViewModel.isPlaying()) {
                    launchUpdateProgressJob()
                }
            }
        })
        dataBinding.buttonSongList.setOnClickListener {
            playerViewModel.getPlayList()?.let {
                val songListDialog = SongListDialog(this).apply {
                    show()
                }
                songListDialog.setSongs(it.songs)
            }
        }
    }

    private fun subscribeService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIFICATION_CODE)
            }
        }
        playerViewModel = ViewModelProvider(this)[MusicPlayerViewModel::class.java].apply {
            serviceBoundLiveData.observe(this@MusicPlayerActivity) { bound ->
                if (bound) {
                    Log.d(TAG, "subscribeService: wyj service bound")
                    registerCallback(this@MusicPlayerActivity)
                    onSongUpdated(getPlayingSong())
                    updatePlayMode(PreferenceManager.lastPlayMode(this@MusicPlayerActivity))
                }
            }
            playModeLiveData.observe(this@MusicPlayerActivity) {
                updatePlayMode(it)
            }
            subscribe()
        }
        dataBinding.playerViewModel = playerViewModel
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
        Log.d(TAG, "onSongUpdated: wyj isPlaying:${playerViewModel.isPlaying()}")
        // - Album rotation
        if (playerViewModel.isPlaying()) {
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
                playerViewModel.let {
                    if (it.isPlaying()) {
                        emit(it.getProgress())
                        delay(50)
                    }
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .onEach {
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
        val currentSong: Song? = playerViewModel.getPlayingSong()
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
        playerViewModel.seekTo(duration)
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
        playerViewModel.unregisterCallback(this)
        playerViewModel.unsubscribe()
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
