package com.wyj.voice.player

enum class PlayMode {
    SINGLE, LOOP, LIST, SHUFFLE;
    companion object {
        fun getDefault(): PlayMode {
            return LOOP
        }

        fun switchNextMode(current: PlayMode): PlayMode {
            return when (current) {
                LOOP -> LIST
                LIST -> SHUFFLE
                SHUFFLE -> SINGLE
                SINGLE -> LOOP
            }
        }
    }
}