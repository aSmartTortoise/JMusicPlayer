package com.wyj.voice.viewModel

import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.wyj.voice.model.Song
import com.wyj.voice.model.SongRepository
import io.reactivex.disposables.CompositeDisposable

class LocalMusicViewModel(var activity: AppCompatActivity) : ViewModel(),
    LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        const val TAG = "LocalMusicViewModel"
        const val URL_LOAD_LOCAL_MUSIC = 0
        private val MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        private const val WHERE = (MediaStore.Audio.Media.IS_MUSIC + "=1 AND "
                + MediaStore.Audio.Media.SIZE + ">0")
        private const val ORDER_BY = MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
        private val PROJECTIONS = arrayOf(
            MediaStore.Audio.Media.DATA,  // the real path
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )
    }

    var songs: MutableLiveData<List<Song>> = MutableLiveData()
    var songRepository: SongRepository? = null
    var comDisposable = CompositeDisposable()

    init {
        songRepository = SongRepository()
    }

    fun getLocalSongs() {
        LoaderManager.getInstance(activity).initLoader(URL_LOAD_LOCAL_MUSIC, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            activity,
            MEDIA_URI,
            PROJECTIONS,
            WHERE,
            null,
            ORDER_BY
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        Log.d(TAG, "onLoadFinished: count:${data?.count}")
        data?.let { cursor ->
            if (cursor.count > 0) {
                songRepository?.getLocalSongs(cursor) { disposable, songs ->
                    comDisposable.add(disposable!!)
                    Log.d(TAG, "onLoadFinished: wyj songs:$songs")
                    for (song in songs) {
                        val index = songs.indexOf(song)
                        if (index == 0) {
                            song.album = "https://www.smugmug.com/photos/i-Nh4X4XM/0/O/i-Nh4X4XM-O.jpg"
                            song.artist = "阿杜"
                        } else if (index == 1) {
                            song.album = "https://d3tvwjfge35btc.cloudfront.net/Assets/59/240/L_p0017924059.jpg"
                            song.artist = "杨宗伟"
                        }
                        song.displayName?.let {
                            if (it.contains('.')) {
                                val lastIndex = it.lastIndexOf('.')
                                song.displayName = it.subSequence(0, lastIndex).toString()
                            }
                        }
                    }
                    this.songs.value = songs
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {

    }
}