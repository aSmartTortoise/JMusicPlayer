package com.wyj.voice.ui

import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wyj.voice.R
import com.wyj.voice.utils.GradientUtils

class MusicPlayerActivity : AppCompatActivity() {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // https://crazygui.wordpress.com/2010/09/05/high-quality-radial-gradient-in-android/
        val displayMetrics = resources.displayMetrics
        // int screenWidth = displayMetrics.widthPixels;
        val screenHeight = displayMetrics.heightPixels
        val window = window
        val gradientBackgroundDrawable: GradientDrawable = GradientUtils.create(
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_gradientColor),
            ContextCompat.getColor(this, R.color.mp_theme_dark_blue_background),
            screenHeight / 2,  // (int) Math.hypot(screenWidth / 2, screenHeight / 2),
            0.5f,
            0.5f
        )
        window.setBackgroundDrawable(gradientBackgroundDrawable)
        window.setFormat(PixelFormat.RGBA_8888)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
    }


}