package com.wyj.voice.model

import android.annotation.SuppressLint
import android.database.Cursor
import android.provider.MediaStore
import com.wyj.voice.utils.FileUtils
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import java.io.File


class SongRepository() {


    fun getLocalSongs(data: Cursor, block: (Disposable?, List<Song>) -> Unit) {
        var disposable: Disposable? = null
        Observable.create { emitter ->
            val songArray = mutableListOf<Song>()
            data.moveToFirst()
            var id = 0
            do {
                val song = cursorToMusic(data).apply {
                    this.id = id
                    id = id.inc()
                }
                songArray.add(song)
            } while (data.moveToNext())
            emitter.onNext(songArray)
            emitter.onComplete()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<List<Song>> {
                override fun onSubscribe(d: Disposable) {
                    disposable = d
                }

                override fun onNext(songs: List<Song>) {
                    block.invoke(disposable, songs)
                }

                override fun onError(e: Throwable) {
                }

                override fun onComplete() {

                }
            })
    }

    private fun cursorToMusic(cursor: Cursor): Song {
        val realPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        val songFile = File(realPath)
        var song: Song?
        if (songFile.exists()) {
            // Using song parsed from file to avoid encoding problems
            song = FileUtils.fileToMusic(songFile)
            if (song != null) {
                return song
            }
        }
        val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
        var displayName =
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
        if (displayName.endsWith(".mp3")) {
            displayName = displayName.substring(0, displayName.length - 4)
        }

        val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
        val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        val duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
        val size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
        song = Song(0, title, displayName, artist, album, path, duration, size, false)
        return song
    }


}