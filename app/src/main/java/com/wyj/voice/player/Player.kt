package com.wyj.voice.player

import android.media.MediaPlayer
import android.util.Log
import com.wyj.voice.model.Song
import java.io.IOException

class Player private constructor(): IPlayback, MediaPlayer.OnCompletionListener {
    companion object {
        private const val TAG = "Player"
        @Volatile
        private var instance: Player? = null

        fun getInstance(): Player {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Player()
                    }
                }
            }
            return instance!!
        }
    }

    private var player: MediaPlayer? = null

    private var playList: PlayList? = null

    // Default size 2: for service and UI
    private val callbacks = mutableListOf<IPlayback.Callback>()

    // Player status
    private var paused = false

    init {
        player = MediaPlayer().apply {
            setOnCompletionListener(this@Player)
        }
        playList = PlayList()
    }


    override fun setPlayList(list: PlayList) {
        playList = list
    }

    override fun play(): Boolean {
        if (paused) {
            player!!.start()
            notifyPlayStatusChanged(true)
            return true
        }

        playList?.let {
            if (it.prepare()) {
                val song: Song? = it.getCurrentSong()
                try {
                    player?.apply {
                        reset()
                        setDataSource(song?.path)
                        prepare()
                        start()
                        notifyPlayStatusChanged(true)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "play: ", e)
                    notifyPlayStatusChanged(false)
                    return false
                }
                return true
            }
        }

        return false
    }

    override fun play(list: PlayList): Boolean {
        paused = false
        setPlayList(list)
        return play()
    }

    override fun play(list: PlayList, startIndex: Int): Boolean {
        if (startIndex < 0 || startIndex >= list.numOfSongs) return false
        paused = false
        list.playingIndex = startIndex
        setPlayList(list)
        return play()
    }

    override fun play(song: Song): Boolean {
        paused = false
        playList?.apply {
            songs.clear()
            songs.add(song)
        }
        return play()
    }

    override fun playLast(): Boolean {
        paused = false
        val hasLast = playList!!.hasLast()
        if (hasLast) {
            playList!!.last()?.apply {
                play()
                notifyPlayLast(this)
                return true
            }
        }
        return false
    }

    override fun playNext(): Boolean {
        paused = false
        val hasNext = playList!!.hasNext(false)
        if (hasNext) {
            playList!!.next()?.apply {
                play()
                notifyPlayNext(this)
                return true
            }

        }
        return false
    }

    override fun pause(): Boolean {
        player?.let {
            if (isPlaying()) {
                pause()
                paused = true
                notifyPlayStatusChanged(false)
                return true
            }
        }
        return false
    }

    override fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    override fun getProgress(): Int {
        return player?.currentPosition ?: 0
    }

    override fun getPlayingSong(): Song? {
        return playList?.getCurrentSong()
    }

    override fun seekTo(progress: Int): Boolean {
        if (playList?.songs?.isEmpty() == true) return false
        val currentSong: Song? = playList!!.getCurrentSong()
        if (currentSong != null) {
            if (currentSong.duration <= progress) {
                onCompletion(player)
            } else {
                player!!.seekTo(progress)
            }
            return true
        }
        return false
    }

    override fun setPlayMode(playMode: PlayMode) {
        playList?.playMode = playMode
    }


    // Listeners
    override fun onCompletion(mp: MediaPlayer?) {
        var next: Song? = null
        // There is only one limited play mode which is list, player should be stopped when hitting the list end
        playList?.let {
            if (it.playMode === PlayMode.LIST && it.playingIndex === it.numOfSongs - 1) {
                // In the end of the list
                // Do nothing, just deliver the callback
            } else if (it.playMode === PlayMode.SINGLE) {
                next = it.getCurrentSong()
                play()
            } else {
                val hasNext = it.hasNext(true)
                if (hasNext) {
                    next = it.next()
                    play()
                } else {

                }
            }
        }

        notifyComplete(next)
    }

    override fun releasePlayer() {
        playList = null
        player?.let {
            it.reset()
            it.release()
            player = null
        }

        instance = null
    }

    // Callbacks
    override fun registerCallback(callback: IPlayback.Callback) {
        callbacks.add(callback)
    }

    override fun unregisterCallback(callback: IPlayback.Callback) {
        callbacks.remove(callback)
    }

    override fun removeCallbacks() {
        callbacks.clear()
    }

    private fun notifyPlayStatusChanged(isPlaying: Boolean) {
        for (callback in callbacks) {
            callback.onPlayStatusChanged(isPlaying)
        }
    }

    private fun notifyPlayLast(song: Song) {
        for (callback in callbacks) {
            callback.onSwitchLast(song)
        }
    }

    private fun notifyPlayNext(song: Song) {
        for (callback in callbacks) {
            callback.onSwitchNext(song)
        }
    }

    private fun notifyComplete(song: Song?) {
        for (callback in callbacks) {
            callback.onComplete(song)
        }
    }
}