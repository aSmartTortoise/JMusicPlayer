package com.wyj.voice.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wyj.voice.ui.MusicPlayerActivity

class PlayerReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PlayerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: wyj")
        val activityIntent = Intent(Intent.ACTION_VIEW)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activityIntent.setClass(context, MusicPlayerActivity::class.java)
        context.startActivity(activityIntent)
    }
}