package com.wyj.voice.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

object BitmapUtils {
    fun zoomImg(bm: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = bm.width
        val srcHeight = bm.height
        val widthScale = targetWidth * 1.0f / srcWidth
        val heightScale = targetHeight * 1.0f / srcHeight
        val matrix = Matrix()
        matrix.postScale(widthScale, heightScale, 0F, 0F)
        // 如需要可自行设置 Bitmap.Config.RGB_8888 等等
        val bmpRet = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmpRet)
        val paint = Paint()
        canvas.drawBitmap(bm, matrix, paint)
        return bmpRet
    }
}