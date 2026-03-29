package com.wyj.voice.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.LogUtils
import com.wyj.voice.R
import com.wyj.voice.databinding.ActivityMainBinding
import com.wyj.voice.model.Song
import com.wyj.voice.player.IPlayback
import com.wyj.voice.ui.music.MusicPlayerActivity
import com.wyj.voice.ui.music.SongListDialog
import com.wyj.voice.ui.view.MusicPlayerBar
import com.wyj.voice.utils.BarUtils
import com.wyj.voice.viewModel.LocalMusicViewModel
import com.wyj.voice.viewModel.MusicPlayerViewModel


class MainActivity : AppCompatActivity(), View.OnClickListener, MusicPlayerBar.PlayCallback,
    IPlayback.Callback {
    private lateinit var musicViewModel: LocalMusicViewModel
    private lateinit var playerViewModel: MusicPlayerViewModel
    private lateinit var dataBinding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
        const val REQ_PER_CODE = 1
        const val REQ_NOTIFICATION_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.d("onCreate")
        BarUtils.transparentStatusBar(this)
        dataBinding =
            DataBindingUtil.setContentView<ActivityMainBinding?>(this, R.layout.activity_main)
                .apply {
                    titleBar.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)
                    tvLocalMusic.setOnClickListener(this@MainActivity)
                    playerBar.playCallback = this@MainActivity
                }
        initMusicViewModel()
        subscribeService()
        autoLoadMusicIfPermitted()
    }

    private fun initMusicViewModel() {
        musicViewModel = ViewModelProvider(this)[LocalMusicViewModel::class.java].apply {
            songs.observe(this@MainActivity) {
                if (!it.isNullOrEmpty()) {
                    dataBinding.tvLocalMusic.visibility = View.GONE
                }
            }
        }
    }

    private fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, getStoragePermission()) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun autoLoadMusicIfPermitted() {
        if (hasStoragePermission()) {
            dataBinding.tvLocalMusic.visibility = View.GONE
            musicViewModel.getLocalSongs()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_local_music -> {
                if (!hasStoragePermission()) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(getStoragePermission()),
                        REQ_PER_CODE
                    )
                } else if (!musicViewModel.hasSongs()) {
                    musicViewModel.getLocalSongs()
                }
            }
        }
    }

    override fun onPlayToggleAction() {
        if (!musicViewModel.hasSongs()) {
            Toast.makeText(this, "先点击本地音乐获取音乐", Toast.LENGTH_LONG).show()
        } else {
            if (!playerViewModel.hasPlayList()) {
                playerViewModel.playSongs(musicViewModel.getSongs()!!)
                dataBinding.playerBar.setSong(playerViewModel.getPlayingSong())
                dataBinding.playerBar.setPlaying(true)
            } else {
                playerViewModel.onPlayToggleAction()
            }
        }
    }

    override fun onShowSongs() {
        playerViewModel.getPlayList()?.let {
            val songListDialog = SongListDialog(this).apply {
                show()
            }
            songListDialog.setSongs(it.songs)
        }
    }

    override fun onOpenMusicPlayer() {
        Intent(this, MusicPlayerActivity::class.java).apply {
            this@MainActivity.startActivity(this)
            overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIFICATION_CODE)
            }
        }
        playerViewModel = ViewModelProvider(this)[MusicPlayerViewModel::class.java].apply {
            serviceBoundLiveData.observe(this@MainActivity) { bound ->
                if (bound) {
                    registerCallback(this@MainActivity)
                    if (!hasPlayList()) {
                        musicViewModel.getSongs()?.let {
                            if (it.isNotEmpty()) {
                                playSongs(it)
                                dataBinding.playerBar.setSong(getPlayingSong())
                                dataBinding.playerBar.setPlaying(true)
                            }
                        }
                    }
                }
            }
            subscribe()
        }
    }

    private fun onSongUpdated(song: Song?) {
        dataBinding.playerBar.setSong(song)
        dataBinding.playerBar.setPlaying(playerViewModel.isPlaying())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PER_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                LogUtils.d("$TAG onRequestPermissionsResult: storage permission not granted.")
            } else {
                dataBinding.tvLocalMusic.visibility = View.GONE
                musicViewModel.getLocalSongs()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerViewModel.unregisterCallback(this)
    }
}
