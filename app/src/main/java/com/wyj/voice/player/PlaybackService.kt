package com.wyj.voice.player

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.wyj.voice.R
import com.wyj.voice.model.Song
import com.wyj.voice.ui.TrampolineActivity
import com.wyj.voice.utils.AlbumUtils

class PlaybackService : Service(), IPlayback, IPlayback.Callback {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val TAG = "PlaybackService"
    }

    private lateinit var player: Player
    private var mContentViewBig: RemoteViews? = null
    private var mContentViewSmall: RemoteViews? = null
    private var registerPlaybackCallback: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: wyj")
        player = Player.getInstance().apply {
            registerCallback(this@PlaybackService)
            registerPlaybackCallback = true
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind: wyj")
        return LocalBinder()
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind: wyj")
        super.onRebind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            Log.d(TAG, "onStartCommand: wyj action:$action")
            when (action) {
                getString(R.string.action_play_toggle) -> {
                    if (isPlaying()) {
                        pause()
                    } else {
                        play()
                    }
                }
                getString(R.string.action_play_next) -> playNext()
                getString(R.string.action_play_last) -> playLast()
                getString(R.string.action_stop_service) -> {
                    if (isPlaying()) {
                        pause()
                    }
                    stopForeground(true)
                    unregisterCallback(this)
                    registerPlaybackCallback = false
                }
            }
        }
        return START_STICKY
    }

    fun registerPlaybackCallback() {
        if (!registerPlaybackCallback) {
            player.registerCallback(this@PlaybackService)
            registerPlaybackCallback = true
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: wyj")
        return super.onUnbind(intent)
    }

    override fun stopService(name: Intent?): Boolean {
        stopForeground(true)
        unregisterCallback(this)
        registerPlaybackCallback = true
        return super.stopService(name)
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        val service: PlaybackService
            get() = this@PlaybackService
    }

    override fun setPlayList(list: PlayList) {
        player.setPlayList(list)
    }

    override fun play(): Boolean {
        return player.play()
    }

    override fun play(list: PlayList): Boolean {
        return player.play(list)
    }

    override fun play(list: PlayList, startIndex: Int): Boolean {
        return player.play(list, startIndex)
    }

    override fun play(song: Song): Boolean {
        return player.play(song)
    }

    override fun playLast(): Boolean {
        return player.playLast()
    }

    override fun playNext(): Boolean {
        Log.d(TAG, "playNext: wyj")
        return player.playNext()
    }

    override fun pause(): Boolean {
        return player.pause()
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying()
    }

    override fun getProgress(): Int {
        return player.getProgress()
    }

    override fun getPlayingSong(): Song? {
        return player.getPlayingSong()
    }

    override fun seekTo(progress: Int): Boolean {
        return player.seekTo(progress)
    }

    override fun setPlayMode(playMode: PlayMode) {
        player.setPlayMode(playMode)
    }

    override fun registerCallback(callback: IPlayback.Callback) {
        player.registerCallback(callback)
    }

    override fun unregisterCallback(callback: IPlayback.Callback) {
        player.unregisterCallback(callback)
    }

    override fun removeCallbacks() {
        player.removeCallbacks()
    }

    override fun releasePlayer() {
        player.releasePlayer()
    }

    override fun onSwitchLast(last: Song) {
        showNotification()
    }

    override fun onSwitchNext(next: Song) {
        showNotification()
    }

    override fun onComplete(next: Song?) {
        showNotification()
    }

    override fun onPlayStatusChanged(isPlaying: Boolean) {
        showNotification()
    }

    // Notification
    /**
     * Show a notification while this service is running.
     */
    private fun showNotification() {
        // The PendingIntent to launch our activity if the user selects this notification
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }
        val intent = Intent(this, TrampolineActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, flag)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "my_service"
            val chan = NotificationChannel(
                channelId,
                "My Background Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentIntent(contentIntent) // The intent to send when the entry is clicked
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_notification_app_logo) // the status icon
                .setWhen(System.currentTimeMillis()) // the time stamp
                .setCustomContentView(getSmallContentView())
                .setCustomBigContentView(getBigContentView())
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
                .setOngoing(true)
                .build()
            service.notify(NOTIFICATION_ID, notification)

            // Set the info for the views that show in the notification panel.
            // Send the notification.
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getSmallContentView(): RemoteViews? {
        if (mContentViewSmall == null) {
            mContentViewSmall = RemoteViews(packageName, R.layout.remote_view_music_player_small)
            setUpRemoteView(mContentViewSmall!!)
        }
        updateRemoteViews(mContentViewSmall!!)
        return mContentViewSmall
    }

    private fun getBigContentView(): RemoteViews? {
        if (mContentViewBig == null) {
            mContentViewBig = RemoteViews(packageName, R.layout.remote_view_music_player)
            setUpRemoteView(mContentViewBig!!)
        }
        updateRemoteViews(mContentViewBig!!)
        return mContentViewBig
    }

    private fun setUpRemoteView(remoteView: RemoteViews) {
        remoteView.setImageViewResource(R.id.image_view_close, R.drawable.ic_remote_view_close)
        remoteView.setImageViewResource(
            R.id.image_view_play_last,
            R.drawable.ic_remote_view_play_last
        )
        remoteView.setImageViewResource(
            R.id.image_view_play_next,
            R.drawable.ic_remote_view_play_next
        )
        remoteView.setOnClickPendingIntent(
            R.id.button_close,
            getPendingIntent(getString(R.string.action_stop_service))
        )
        remoteView.setOnClickPendingIntent(
            R.id.button_play_last,
            getPendingIntent(getString(R.string.action_play_last))
        )
        remoteView.setOnClickPendingIntent(
            R.id.button_play_next,
            getPendingIntent(getString(R.string.action_play_next))
        )
        remoteView.setOnClickPendingIntent(
            R.id.button_play_toggle,
            getPendingIntent(getString(R.string.action_play_toggle))
        )
    }

    private fun updateRemoteViews(remoteView: RemoteViews) {
        val currentSong: Song? = player.getPlayingSong()
        if (currentSong != null) {
            remoteView.setTextViewText(R.id.text_view_name, currentSong.displayName)
            remoteView.setTextViewText(R.id.text_view_artist, currentSong.artist)
        }
        remoteView.setImageViewResource(
            R.id.image_view_play_toggle,
            if (isPlaying()) R.drawable.ic_remote_view_pause else R.drawable.ic_remote_view_play
        )
        val album: Bitmap? = getPlayingSong()?.let { AlbumUtils.parseAlbum(it) }
        if (album == null) {
            remoteView.setImageViewResource(R.id.image_view_album, R.mipmap.ic_launcher)
        } else {
            remoteView.setImageViewBitmap(R.id.image_view_album, album)
        }
    }

    // PendingIntent
    private fun getPendingIntent(action: String): PendingIntent {
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flag = PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getService(this, 0, Intent(action), flag)
    }
}