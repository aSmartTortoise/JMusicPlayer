package com.wyj.voice.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.wyj.voice.R
import com.wyj.voice.databinding.ActivityMainBinding
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.IPlayback
import com.wyj.voice.player.PlayList
import com.wyj.voice.utils.BarUtils
import com.wyj.voice.view.MusicPlayerBar
import com.wyj.voice.viewModel.LocalMusicViewModel
import com.wyj.voice.viewModel.MusicPlayerViewModel


class MainActivity : AppCompatActivity(), View.OnClickListener, MusicPlayerBar.PlayCallback,
    IPlayback.Callback {
    private var musicViewModel: LocalMusicViewModel? = null
    private var playerViewModel: MusicPlayerViewModel? = null
    private var localSongs: List<Song>? = null
    private var player: IPlayback? = null
    private lateinit var dataBinding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
        const val REQ_PER_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: wyj")
        setContentView(R.layout.activity_main)
        BarUtils.transparentStatusBar(this)
        dataBinding =
            DataBindingUtil.setContentView<ActivityMainBinding?>(this, R.layout.activity_main)
                .apply {
                    titleBar.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)
                    tvLocalMusic.setOnClickListener(this@MainActivity)
                    playerBar.playCallback = this@MainActivity
                    btnCommon.setOnClickListener(this@MainActivity)
                }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_local_music -> {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQ_PER_CODE
                    )
                } else {
                    if (localSongs == null || localSongs?.isEmpty() == true) {
                        getLocalMusic()
                    }
                }
            }
            R.id.btn_common -> {
                startActivity(Intent(this, CommonActivity::class.java))
            }
        }
    }

    private fun getLocalMusic() {
        musicViewModel = LocalMusicViewModel(this).apply {
            songs.observe(this@MainActivity) {
                localSongs = it
            }
            getLocalSongs()
        }
    }

    override fun onPlayToggleAction() {
        if (localSongs == null) {
            Toast.makeText(this, "先点击本地音乐获取音乐", Toast.LENGTH_LONG).show()
        } else {
            if (player == null) {
                subscribeService()
            } else {
                player?.let {
                    if (it.isPlaying()) it.pause() else {
                        it.play()
                    }
                }
            }
        }
    }

    override fun onShowSongs() {
        Log.d(TAG, "onShowSongs: ")
    }

    override fun onOpenMusicPlayer() {
        if (player != null) {
            Intent(this, MusicPlayerActivity::class.java).apply {
                this@MainActivity.startActivity(this)
            }
        }
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
        dataBinding.playerBar.setPlaying(isPlaying)
    }

    private fun subscribeService() {
        if (player == null) {
            playerViewModel = MusicPlayerViewModel(this).apply {
                playbackServiceLiveData.observe(this@MainActivity) { player ->
                    this@MainActivity.player = player.apply {
                        registerCallback(this@MainActivity)
                    }
                    localSongs?.let {
                        if (it.isNotEmpty()) {
                            playSong(it)
                            dataBinding.playerBar.setSong(player?.getPlayingSong())
                            dataBinding.playerBar.setPlaying(true)
                        }
                    }
                }
                subscribe()
            }
        }
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
        dataBinding.playerBar.setSong(song)
        dataBinding.playerBar.setPlaying(player?.isPlaying()?: false)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PER_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: storage permission not granted.")
            } else {
                getLocalMusic()
            }
        }
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

        if (player != null && playerViewModel != null) {
            playerViewModel?.unsubscribe()
            playerViewModel = null
            player = null
        }
    }




}