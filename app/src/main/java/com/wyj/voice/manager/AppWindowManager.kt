package com.wyj.voice.manager

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager


object AppWindowManager {

    fun transparentNavBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT)
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
}