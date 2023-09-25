package com.wyj.voice.player

import com.wyj.voice.model.Folder
import com.wyj.voice.model.Song
import java.util.*

class PlayList() {
    companion object {
        // Play List: Favorite
        const val NO_POSITION = -1
        const val COLUMN_FAVORITE = "favorite"

        fun fromFolder(folder: Folder): PlayList {
            val playList = PlayList()
            playList.name = folder.name
            playList.songs = folder.songs
            playList.numOfSongs = folder.numOfSongs
            return playList
        }
    }

    var id = 0
        get() = field
        set(value) {
            field = value
        }
    var name: String? = null
        get() = field
        set(value) {
            field = value
        }

    var numOfSongs = 0
        get() = field
        set(value) {
            field = value
        }

    var favorite = false
        get() = field
        set(value) {
            field = value
        }

    var createdAt: Date? = null
        get() = field
        set(value) {
            field = value
        }

    var updatedAt: Date? = null
        get() = field
        set(value) {
            field = value
        }
    var playingIndex = -1
        get() = field
        set(value) {
            field = value
        }
    var songs: MutableList<Song> = mutableListOf()
        get() = field
        set(value) {
            field = value
        }
    var playMode = PlayMode.LOOP
        get() = field
        set(value) {
            field = value
        }

    public constructor(song: Song): this() {
        songs.add(song)
        numOfSongs = 1
    }

    // Utils
    fun getItemCount(): Int {
        return songs.size
    }

    fun addSong(song: Song) {
        songs.add(song)
        numOfSongs = songs.size
    }

    fun addSong(song: Song, index: Int) {
        songs.add(index, song)
        numOfSongs = songs.size
    }

    fun addSong(songs: List<Song>, index: Int) {
        this.songs.addAll(index, songs)
        numOfSongs = this.songs.size
    }

    fun removeSong(song: Song): Boolean {
        var index: Int
        if (songs.indexOf(song).also { index = it } != -1) {
            if (songs.removeAt(index) != null) {
                numOfSongs = songs.size
                return true
            }
        } else {
            val iterator = songs.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (song.path.equals(item.path)) {
                    iterator.remove()
                    numOfSongs = songs.size
                    return true
                }
            }
        }
        return false
    }

    /**
     * Prepare to play
     */
    fun prepare(): Boolean {
        if (songs.isEmpty()) return false
        if (playingIndex == NO_POSITION) {
            playingIndex = 0
        }
        return true
    }

    /**
     * The current song being played or is playing based on the [.playingIndex]
     */
    fun getCurrentSong(): Song? {
        return if (playingIndex != NO_POSITION) {
            songs[playingIndex]
        } else null
    }

    fun hasLast(): Boolean {
        return songs.size != 0
    }

    fun last(): Song? {
        when (playMode) {
            PlayMode.LOOP, PlayMode.LIST, PlayMode.SINGLE -> {
                var newIndex = playingIndex - 1
                if (newIndex < 0) {
                    newIndex = songs.size - 1
                }
                playingIndex = newIndex
            }
            PlayMode.SHUFFLE -> playingIndex = randomPlayIndex()
        }
        return songs[playingIndex]
    }

    /**
     * @return Whether has next song to play.
     *
     *
     * If this query satisfies these conditions
     * - comes from media player's complete listener
     * - current play mode is PlayMode.LIST (the only limited play mode)
     * - current song is already in the end of the list
     * then there shouldn't be a next song to play, for this condition, it returns false.
     *
     *
     * If this query is from user's action, such as from play controls, there should always
     * has a next song to play, for this condition, it returns true.
     */
    fun hasNext(fromComplete: Boolean): Boolean {
        if (songs.isEmpty()) return false
        if (fromComplete) {
            if (playMode === PlayMode.LIST && playingIndex + 1 >= songs.size) return false
        }
        return true
    }

    /**
     * Move the playingIndex forward depends on the play mode
     *
     * @return The next song to play
     */
    operator fun next(): Song? {
        when (playMode) {
            PlayMode.LOOP, PlayMode.LIST, PlayMode.SINGLE -> {
                var newIndex = playingIndex + 1
                if (newIndex >= songs.size) {
                    newIndex = 0
                }
                playingIndex = newIndex
            }
            PlayMode.SHUFFLE -> playingIndex = randomPlayIndex()
        }
        return songs[playingIndex]
    }

    private fun randomPlayIndex(): Int {
        val randomIndex = Random().nextInt(songs.size)
        // Make sure not play the same song twice if there are at least 2 songs
        if (songs.size > 1 && randomIndex == playingIndex) {
            randomPlayIndex()
        }
        return randomIndex
    }


}