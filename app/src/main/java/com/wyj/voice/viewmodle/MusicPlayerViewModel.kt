package com.wyj.voice.viewmodle

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wyj.voice.manager.PreferenceManager
import com.wyj.voice.player.PlayMode
import com.wyj.voice.player.PlaybackService
import io.reactivex.disposables.CompositeDisposable

class MusicPlayerViewModel(var context: Context?): ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private var isServiceBound = false
    private var comDisposable: CompositeDisposable? = null
    var playbackServiceLiveData = MutableLiveData<PlaybackService>()

    init {
        comDisposable = CompositeDisposable()
    }

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val playbackService: PlaybackService = (service as PlaybackService.LocalBinder).service
            playbackServiceLiveData.value = playbackService
//            mView.onPlaybackServiceBound(mPlaybackService)
//            mView.onSongUpdated(mPlaybackService.getPlayingSong())
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            playbackServiceLiveData.value = null
//            mView.onPlaybackServiceUnbound()
        }
    }

    fun subscribe() {
        bindPlaybackService()
        retrieveLastPlayMode()

        // TODO
//        playbackService?.let {
//            if (it.isPlaying()) {
////                mView.onSongUpdated(mPlaybackService.getPlayingSong())
//            }
//        }
    }

    private fun bindPlaybackService() {
        context?.bindService(Intent(context, PlaybackService::class.java), connection, Context.BIND_AUTO_CREATE)
        isServiceBound = true
    }

    fun retrieveLastPlayMode() {
        val lastPlayMode: PlayMode = PreferenceManager.lastPlayMode(context!!)
//        mView.updatePlayMode(lastPlayMode)
    }

    fun unsubscribe() {
        unbindPlaybackService()
        // Release context reference
        context = null
//        mView = null
        comDisposable?.clear()
    }

    fun unbindPlaybackService() {
        if (isServiceBound) {
            // Detach our existing connection.
            context?.unbindService(connection)
            isServiceBound = false
        }
    }
}