package com.wyj.voice.viewModel

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.IPlayback
import com.wyj.voice.player.PlayList
import com.wyj.voice.player.PlayMode
import com.wyj.voice.player.PlaybackService

class MusicPlayerViewModel(private val context: Context): ViewModel() {
    companion object {
        private const val TAG = "MusicPlayerViewModel"
    }
    @SuppressLint("StaticFieldLeak")
    private var isServiceBind = false
    private var player: PlaybackService? = null
    var serviceBoundLiveData = MutableLiveData<Boolean>()
    var playModeLiveData = MutableLiveData<PlayMode>()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            player = (service as PlaybackService.LocalBinder).service
            serviceBoundLiveData.value = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            player = null
            serviceBoundLiveData.value = false
        }
    }

    fun subscribe() {
        bindPlaybackService()
        retrieveLastPlayMode()
    }

    private fun bindPlaybackService() {
        context.bindService(
            Intent(context, PlaybackService::class.java),
            connection,
            Context.BIND_AUTO_CREATE)
        isServiceBind = true
    }

    private fun retrieveLastPlayMode() {
        PreferenceManager.lastPlayMode(context)
    }

    fun registerCallback(callback: IPlayback.Callback) {
        player?.registerCallback(callback)
    }

    fun unregisterCallback(callback: IPlayback.Callback) {
        player?.unregisterCallback(callback)
    }

    fun playSong(song: Song) {
        val playList = PlayList(song)
        playSong(playList, 0)
    }

    fun playSongs(songs: List<Song>) {
        val playList = PlayList()
        playList.addSong(songs, 0)
        playSong(playList, 0)
    }

    fun playSong(playList: PlayList, playIndex: Int) {
        playList.playMode = PreferenceManager.lastPlayMode(context)
        player?.play(playList, playIndex)
    }

    fun getPlayingSong(): Song? = player?.getPlayingSong()

    fun getPlayList(): PlayList? = player?.getPlayList()

    fun hasPlayList(): Boolean = !player?.getPlayList()?.songs.isNullOrEmpty()

    fun isPlaying(): Boolean = player?.isPlaying() ?: false

    fun seekTo(progress: Int) {
        player?.seekTo(progress)
    }

    fun getProgress(): Int = player?.getProgress() ?: 0

    fun onPlayToggleAction() {
        player?.let {
            if (it.isPlaying()) it.pause() else {
                it.registerPlaybackCallback()
                it.play()
            }
        }
    }

    fun playLast() = player?.playLast()

    fun playNext() = player?.playNext()

    fun onPlayModeToggleAction() {
        player?.let {
            val current = PreferenceManager.lastPlayMode(context)
            val newMode = PlayMode.switchNextMode(current)
            PreferenceManager.setPlayMode(context, newMode)
            it.setPlayMode(newMode)
            playModeLiveData.value = newMode
        }
    }

    fun unsubscribe() {
        unbindPlaybackService()
        player = null
    }

    private fun unbindPlaybackService() {
        Log.d(TAG, "unbindPlaybackService: wyj isServiceBind:$isServiceBind")
        if (isServiceBind) {
            context.unbindService(connection)
            isServiceBind = false
        }
    }
}
