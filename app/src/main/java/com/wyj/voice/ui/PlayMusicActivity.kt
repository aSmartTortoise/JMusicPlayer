package com.wyj.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wyj.voice.R
import com.wyj.voice.viewModel.LocalMusicViewModel
import java.io.IOException
import java.util.*


/**
 *  https://www.cnblogs.com/rustfisher/p/11568524.html
 *
 *  https://blog.csdn.net/qq_38436214/article/details/87996625
 *  https://blog.csdn.net/qq_38436214/article/details/88040066
 *
 *  下载mp3
 *  https://tool.liumingye.cn/music/#/artist/zlrQ
 *
 */
class PlayMusicActivity : AppCompatActivity(), View.OnClickListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {
    companion object {
        const val TAG = "PlayMusicActivity"
        const val REQ_PER_CODE = 1
    }

    var mediaPlayer: MediaPlayer? = null
    private var isSeekbarChanging = false  //互斥变量，防止进度条和定时器冲突。
    private var tvProgress: TextView? = null
    private var tvTotalTime: TextView? = null
    private var sbTime: AppCompatSeekBar? = null
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var musicViewModel: LocalMusicViewModel? = null
    private var playingIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_music)
        findViewById<View>(R.id.btn_play).setOnClickListener(this)
        findViewById<View>(R.id.btn_pause).setOnClickListener(this)
        findViewById<View>(R.id.btn_stop).setOnClickListener(this)
        tvProgress = findViewById<TextView>(R.id.tv_progress)
        tvTotalTime = findViewById<TextView>(R.id.tv_total_time)
        sbTime = findViewById<AppCompatSeekBar>(R.id.sb_time).apply {
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    mediaPlayer?.let {
                        val duration = it.duration / 1000//获取音乐总时长
                        val position = it.currentPosition//获取当前播放的位置
                        tvProgress?.text = calculateTime(position / 1000)//开始时间
                        tvTotalTime?.text = calculateTime(duration)//总时长
                    }

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeekbarChanging = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isSeekbarChanging = false
                    mediaPlayer?.let {
                        it.seekTo(seekBar?.progress ?: 0)//在当前位置播放
                        tvProgress?.text = calculateTime(it.currentPosition / 1000)
                    }
                }
            })
        }

        musicViewModel = LocalMusicViewModel(this).apply {
            songs.observe(this@PlayMusicActivity) {
                for (song in it) {
                    val path = song.path
                    Log.d(TAG, "onCreate: path:$path")
                }
            }
            getLocalSongs()
        }
    }

    private fun calculateTime(time: Int): String {
        var minute = 0
        var second = 0
        if (time >= 60) {
            minute = time / 60;
            second = time % 60;
            //分钟在0~9
            return if (minute < 10) {
                //判断秒
                if (second < 10) {
                    "0$minute:0$second"
                } else {
                    "0$minute:$second"
                }
            } else {
                //分钟大于10再判断秒
                if (second < 10) {
                    "$minute:0$second"
                } else {
                    "$minute:$second"
                }
            }
        } else {
            second = time;
            return if (second in 0..9) {
                "00:0$second"
            } else {
                "00:$second"
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_play -> {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQ_PER_CODE
                    )
                } else {
                    if (musicViewModel?.songs?.value?.isNotEmpty() == true) {
                        initPlayerAndPlay()
                    }
                }
            }

            R.id.btn_pause -> {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        timerTask?.cancel()
                    }
                }
            }

            R.id.btn_stop -> {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.reset()
                        timerTask?.cancel()
                    }
                }
            }
        }
    }

    private fun initPlayerAndPlay() {
        createPlayerAndInit()
        mediaPlayer?.apply {
            if (!isPlaying) {
                reset()
                prepareToPlay()
                try {
                    start()
                } catch (e: IllegalStateException) {
                    Log.d(TAG, "play: start error e:${e.message}")
                    return
                }

                sbTime?.max = duration //将音乐总时间设置为Seekbar的最大值
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
                            if (!isSeekbarChanging) {
                                try {
                                    sbTime?.progress = currentPosition
                                } catch (e: IllegalStateException) {
                                    Log.d(TAG, "run: e:${e.message}")
                                }
                            }
                        }
                    }
                }
                timer?.schedule(timerTask, 0, 50)
            }
        }
    }

    private fun createPlayerAndInit() {
        if (mediaPlayer == null) {
            volumeControlStream = AudioManager.STREAM_MUSIC
//            volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener(this@PlayMusicActivity)
                setOnCompletionListener(this@PlayMusicActivity)
                isLooping = false
                setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
        }
    }

    private fun MediaPlayer.prepareToPlay() {
        try {
            setDataSource(musicViewModel?.songs?.value!![playingIndex].path)
        } catch (e: IOException) {
            Log.d(TAG, "prepareToPlay: setDataSource error e:${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "prepareToPlay: setDataSource error e:${e.message}")
        } catch (e: SecurityException) {
            Log.d(TAG, "prepareToPlay: setDataSource error e:${e.message}")
        } catch (e: IllegalStateException) {
            Log.d(TAG, "prepareToPlay: setDataSource error e:${e.message}")
        }

        try {
            prepare()
        } catch (e: IOException) {
            Log.d(TAG, "prepareToPlay: prepare error e:${e.message}")
        } catch (e: IllegalStateException) {
            Log.d(TAG, "prepareToPlay: prepare error e:${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PER_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: storage permission granted.")
            }
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.d(TAG, "onError: what:$what, extra:$extra")
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(TAG, "onCompletion: ")
        mediaPlayer?.let {
            Log.d(TAG, "onCompletion: it is playing? ${it.isPlaying}")
        }
        timerTask?.cancel()
        musicViewModel?.songs?.value?.let {
            var newIndex = playingIndex + 1
            if (newIndex >= it.size) {
                newIndex = 0
            }
            playingIndex = newIndex
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        mediaPlayer?.let {
            it.stop()
            it.release()
            mediaPlayer = null
        }
        timerTask?.let {
            it.cancel()
            timerTask = null
        }
        timer = null
        musicViewModel?.let {
            if (it.comDisposable.isDisposed) {
                it.comDisposable.dispose()
                it.comDisposable.clear()
                musicViewModel = null
            }
        }
    }
}