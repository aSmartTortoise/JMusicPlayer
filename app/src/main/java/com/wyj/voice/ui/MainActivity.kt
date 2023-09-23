package com.wyj.voice.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wyj.voice.R


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_to_play).setOnClickListener {
            Intent(this, PlayMusicActivity::class.java).apply {
                this@MainActivity.startActivity(this)
            }
        }

        findViewById<View>(R.id.btn_to_player).setOnClickListener {
            Intent(this, MusicPlayerActivity::class.java).apply {
                this@MainActivity.startActivity(this)
            }
        }


    }
}