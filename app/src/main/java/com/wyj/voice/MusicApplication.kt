package com.wyj.voice

import android.app.Application
import android.util.Log
import com.blankj.utilcode.util.LogUtils


/**
 *  author : jie wang
 *  date : 2024/3/12 19:36
 *  description :
 */
class MusicApplication : Application() {
    companion object {
        private const val TAG = "MusicApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate:")
        val logDir = (externalCacheDir ?: cacheDir).absolutePath + "/logs"
        LogUtils.getConfig()
            .setDir(logDir)
            .setBorderSwitch(false)
            .setLogHeadSwitch(false)
        LogUtils.d("onCreate, log dir:$logDir")
    }
}