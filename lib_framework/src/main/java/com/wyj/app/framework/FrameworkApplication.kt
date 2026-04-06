package com.wyj.app.framework

import android.app.Application
import android.content.Context
import com.blankj.utilcode.util.LogUtils
import com.hm.iou.lifecycle.annotation.AppLifecycle
import com.hm.lifecycle.api.IApplicationLifecycleCallbacks
import com.wyj.app.framework.helper.AppHelper
import com.wyj.app.framework.util.DnnUtils

/**
 *  author : jie wang
 *  date : 2024/4/17 15:54
 *  description :
 */
@AppLifecycle
class FrameworkApplication() : IApplicationLifecycleCallbacks {

    override fun getPriority() = IApplicationLifecycleCallbacks.MAX_PRIORITY

    override fun onCreate(context: Context?) {
        LogUtils.d("onCreate")
        AppHelper.init(context!! as Application, BuildConfig.DEBUG)
        DnnUtils.switchToAllNetwork(context)
    }

    override fun onTerminate() {

    }

    override fun onLowMemory() {
    }

    override fun onTrimMemory(level: Int) {
    }
}