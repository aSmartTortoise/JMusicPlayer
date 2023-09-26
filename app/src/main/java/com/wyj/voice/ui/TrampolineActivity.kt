package com.wyj.voice.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 *  https://developer.android.com/about/versions/12/behavior-changes-12?hl=zh-cn#notification-trampolines
 *  Android S， 通知Trampoline特性。
 */
class TrampolineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MusicPlayerActivity::class.java))
        finish()
    }
}