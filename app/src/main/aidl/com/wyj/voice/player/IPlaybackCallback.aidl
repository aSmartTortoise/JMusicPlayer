package com.wyj.voice.player;
import com.wyj.voice.model.Song;

interface IPlaybackCallback {
    void onSwitchLast(in Song last);
    void onSwitchNext(in Song next);
    void onComplete(in Song next);
    void onPlayStatusChanged(boolean isPlaying);
}
