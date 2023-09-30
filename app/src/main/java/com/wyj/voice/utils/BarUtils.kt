package com.wyj.voice.utils

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt

object BarUtils {

    ///////////////////////////////////////////////////////////////////////////
    // status bar
    ///////////////////////////////////////////////////////////////////////////
    private const val TAG_STATUS_BAR = "TAG_STATUS_BAR"

    fun setStatusBarColor(
        activity: Activity,
        @ColorInt color: Int,
        isDecor: Boolean
    ): View? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return null
        transparentStatusBar(activity)
        return applyStatusBarColor(activity, color, isDecor)
    }

    fun transparentStatusBar(activity: Activity) {
        val window: Window = activity.window
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            val option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            val vis = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = option or vis
            window.statusBarColor = Color.TRANSPARENT
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    private fun applyStatusBarColor(
        activity: Activity,
        color: Int,
        isDecor: Boolean
    ): View? {
        return applyStatusBarColor(activity.window, color, isDecor)
    }

    private fun applyStatusBarColor(
        window: Window,
        color: Int,
        isDecor: Boolean
    ): View? {
        val parent =
            if (isDecor) window.decorView as ViewGroup else (window.findViewById<View>(R.id.content) as ViewGroup)
        var fakeStatusBarView = parent.findViewWithTag<View>(BarUtils.TAG_STATUS_BAR)
        if (fakeStatusBarView != null) {
            if (fakeStatusBarView.visibility == View.GONE) {
                fakeStatusBarView.visibility = View.VISIBLE
            }
            fakeStatusBarView.setBackgroundColor(color)
        } else {
            fakeStatusBarView = createStatusBarView(window.context, color)
            parent.addView(fakeStatusBarView)
        }
        return fakeStatusBarView
    }

    private fun createStatusBarView(
        context: Context,
        color: Int
    ): View? {
        val statusBarView = View(context)
        statusBarView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight()
        )
        statusBarView.setBackgroundColor(color)
        statusBarView.tag = TAG_STATUS_BAR
        return statusBarView
    }

    fun getStatusBarHeight(): Int {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    fun transparentNavBar(activity: Activity) {
        val window: Window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (window.getAttributes().flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION === 0) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            }
        }
        val decorView: View = window.decorView
        val vis: Int = decorView.getSystemUiVisibility()
        val option: Int =
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        decorView.setSystemUiVisibility(vis or option)
    }

    @SuppressLint("InternalInsetResource")
    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId: Int =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}