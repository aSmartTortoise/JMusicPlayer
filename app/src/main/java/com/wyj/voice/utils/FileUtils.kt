package com.wyj.voice.utils

import android.media.MediaMetadataRetriever
import android.text.TextUtils
import com.wyj.voice.model.Song
import java.io.File

object FileUtils {

    private const val UNKNOWN = "unknown"

    fun fileToMusic(file: File): Song? {
        if (file.length() == 0L) return null
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(file.absolutePath)
        val duration: Int
        val keyDuration =
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        // ensure the duration is a digit, otherwise return null song
        if (keyDuration == null || !keyDuration.matches(Regex("\\d+"))) return null
        duration = keyDuration.toInt()
        val title = extractMetadata(
            metadataRetriever,
            MediaMetadataRetriever.METADATA_KEY_TITLE,
            file.name
        )
        val displayName = extractMetadata(
            metadataRetriever,
            MediaMetadataRetriever.METADATA_KEY_TITLE,
            file.name
        )
        val artist = extractMetadata(
            metadataRetriever,
            MediaMetadataRetriever.METADATA_KEY_ARTIST,
            UNKNOWN
        )
        val album = extractMetadata(
            metadataRetriever,
            MediaMetadataRetriever.METADATA_KEY_ALBUM,
            UNKNOWN
        )
        return Song(
            0, title, displayName, artist, album, file.absolutePath, duration,
            file.length().toInt()
        )
    }


    private fun extractMetadata(retriever: MediaMetadataRetriever, key: Int, defaultValue: String): String? {
        var value = retriever.extractMetadata(key)
        if (TextUtils.isEmpty(value)) {
            value = defaultValue
        }
        return value
    }
}