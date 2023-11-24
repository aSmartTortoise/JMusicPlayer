package com.wyj.voice.viewModel

import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.wyj.voice.model.Song
import com.wyj.voice.model.SongRepository
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
    var comDisposable = CompositeDisposable()

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
                SongRepository.getLocalSongsFlow(cursor)
                    .onEach {
                        this.songs.value = it
                    }
                    .flowOn(Dispatchers.Main)
                    .launchIn(viewModelScope)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {

    }
}