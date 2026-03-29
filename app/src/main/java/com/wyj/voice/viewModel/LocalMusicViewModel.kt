package com.wyj.voice.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wyj.voice.model.Song
import com.wyj.voice.model.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocalMusicViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "LocalMusicViewModel"
    }

    var songs: MutableLiveData<List<Song>> = MutableLiveData()

    fun getSongs(): List<Song>? = songs.value

    fun hasSongs(): Boolean = !songs.value.isNullOrEmpty()

    fun getLocalSongs() {
        SongRepository.queryLocalSongsFlow(getApplication())
            .onEach {
                this.songs.value = it
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }
}
