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
    private var playService: PlaybackService.LocalBinder? = null
    private var registeredCallback: IPlayback.Callback? = null
    var serviceBoundLiveData = MutableLiveData<Boolean>()
    var playModeLiveData = MutableLiveData<PlayMode>()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            playService = service as PlaybackService.LocalBinder
            serviceBoundLiveData.value = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            playService = null
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
        playService?.registerCallback(callback)
    }

    fun unregisterCallback(callback: IPlayback.Callback) {
        registeredCallback = null
        playService?.unregisterCallback(callback)
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
        playService?.play(playList, playIndex)
    }

    fun getPlayingSong(): Song? = playService?.getPlayingSong()

    fun getPlayList(): PlayList? = playService?.getPlayList()

    fun hasPlayList(): Boolean = !playService?.getPlayList()?.songs.isNullOrEmpty()

    fun isPlaying(): Boolean = playService?.isPlaying() ?: false

    fun seekTo(progress: Int) {
        playService?.seekTo(progress)
    }

    fun getProgress(): Int = playService?.getProgress() ?: 0

    fun onPlayToggleAction() {
        playService?.let {
            if (it.isPlaying()) it.pause() else {
                it.registerPlaybackCallback()
                it.play()
            }
        }
    }

    fun playLast() = playService?.playLast()

    fun playNext() = playService?.playNext()

    fun onPlayModeToggleAction() {
        playService?.let {
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
            playService?.unregisterCallback(it)
            registeredCallback = null
        }
        unbindPlaybackService()
        playService = null
    }

    private fun unbindPlaybackService() {
        if (isServiceBind) {
            context.unbindService(connection)
            isServiceBind = false
        }
    }
}
