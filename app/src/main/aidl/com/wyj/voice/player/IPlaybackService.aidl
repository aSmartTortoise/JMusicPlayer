package com.wyj.voice.player;
import com.wyj.voice.model.Song;
import com.wyj.voice.player.PlayList;
import com.wyj.voice.player.IPlaybackCallback;

interface IPlaybackService {
    boolean play();
    boolean playWithList(in PlayList list, int startIndex);
    boolean pause();
    boolean playLast();
    boolean playNext();
    boolean isPlaying();
    int getProgress();
    boolean seekTo(int progress);
    Song getPlayingSong();
    PlayList getPlayList();
    void setPlayMode(int playMode);
    void registerCallback(IPlaybackCallback callback);
    void unregisterCallback(IPlaybackCallback callback);
    void registerPlaybackCallback();
}
