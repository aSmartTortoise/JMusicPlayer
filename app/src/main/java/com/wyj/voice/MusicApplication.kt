package com.wyj.voice

import android.app.Application
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import kotlin.math.log

/**
 *  author : jie wang
 *  date : 2024/3/12 19:36
 *  description :
 */
class MusicApplication : Application() {
    val TAG = MusicApplication::class.java.simpleName
    companion object {

    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate:")
        LogUtils.getConfig()
            .setDir(externalCacheDir?.absolutePath + "/logs")
            .setBorderSwitch(false)
            .isLogHeadSwitch = false
        LogUtils.d("onCreate, external path:${externalCacheDir?.absolutePath + "/logs"}")
    }
}