package com.wyj.voice.viewModel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.model.Song
import com.wyj.voice.player.IPlayback
import com.wyj.voice.player.PlayList
import com.wyj.voice.player.PlayMode
import com.wyj.voice.player.PlaybackService

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()
    private var isServiceBind = false
    private var player: PlaybackService? = null
    private var registeredCallback: IPlayback.Callback? = null
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
        if (!isServiceBind) {
            bindPlaybackService()
        }
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
        registeredCallback = callback
        player?.registerCallback(callback)
    }

    fun unregisterCallback(callback: IPlayback.Callback) {
        registeredCallback = null
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

    override fun onCleared() {
        super.onCleared()
        registeredCallback?.let {
            player?.unregisterCallback(it)
            registeredCallback = null
        }
        unbindPlaybackService()
        player = null
    }

    private fun unbindPlaybackService() {
        if (isServiceBind) {
            context.unbindService(connection)
            isServiceBind = false
        }
    }
}
