package com.wyj.voice.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wyj.voice.R
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.IPlayback
import com.wyj.voice.player.PlayList
import com.wyj.voice.viewModel.LocalMusicViewModel
import com.wyj.voice.viewModel.MusicPlayerViewModel


class MainActivity : AppCompatActivity() {
    private var musicViewModel: LocalMusicViewModel? = null
    private var playerViewModel: MusicPlayerViewModel? = null
    private var localSongs: List<Song>? = null
    private var player: IPlayback? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_to_play).setOnClickListener {
            Intent(this, PlayMusicActivity::class.java).apply {
                this@MainActivity.startActivity(this)
            }
        }

        findViewById<View>(R.id.btn_to_player).setOnClickListener {
            Intent(this, MusicPlayerActivity::class.java).apply {
                this@MainActivity.startActivity(this)
            }
        }

        musicViewModel = LocalMusicViewModel(this).apply {
            songs.observe(this@MainActivity) {
                localSongs = it
                subscribeService()
            }
            getLocalSongs()
        }
    }

    private fun subscribeService() {
        if (player == null) {
            playerViewModel = MusicPlayerViewModel(this).apply {
                playbackServiceLiveData.observe(this@MainActivity) { player ->
                    this@MainActivity.player = player
                    localSongs?.let {
                        if (it.isNotEmpty()) {
                            playSong(it)
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
//        val song = playList.getCurrentSong()
//        onSongUpdated(song)
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