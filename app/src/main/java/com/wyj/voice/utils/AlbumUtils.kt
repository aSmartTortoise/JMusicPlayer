package com.wyj.voice.utils

import android.graphics.*
import android.media.MediaMetadataRetriever
import android.util.Log
import com.wyj.voice.model.Song
import java.io.File

object AlbumUtils {
    private const val TAG = "AlbumUtils"

    fun parseAlbum(song: Song): Bitmap? {
        return parseAlbum(File(song.path))
    }

    fun parseAlbum(file: File): Bitmap? {
        val metadataRetriever = MediaMetadataRetriever()
        try {
            metadataRetriever.setDataSource(file.absolutePath)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "parseAlbum: ", e)
        }
        val albumData = metadataRetriever.embeddedPicture
        return if (albumData != null) {
            BitmapFactory.decodeByteArray(albumData, 0, albumData.size)
        } else null
    }

    fun getCroppedBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), (
                    bitmap.width / 2).toFloat(), paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output
    }
}