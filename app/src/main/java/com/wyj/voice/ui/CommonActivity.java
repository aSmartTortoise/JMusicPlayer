package com.wyj.voice.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wyj.voice.R;
import com.wyj.voice.utils.BarUtils;

/**
 * Created by GongWen on 17/8/24.
 */

public class CommonActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common);
        BarUtils.INSTANCE.transparentStatusBar(this);
    }
}
