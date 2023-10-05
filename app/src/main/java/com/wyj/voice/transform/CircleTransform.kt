package com.wyj.voice.transform

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest


class CircleTransform() : BitmapTransformation() {
    companion object {
        const val TAG = "CircleBorderTransform"
        val ID = Companion::class.java.name
    }
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return circleCrop(pool, toTransform)
    }

    private fun circleCrop(pool: BitmapPool, toTransform: Bitmap): Bitmap {
        Log.d(TAG, "circleCrop: wyj width:${toTransform.width}, height:${toTransform.height}")
        val size = toTransform.width.coerceAtMost(toTransform.height)
        val x = (toTransform.width - size) / 2
        val y = (toTransform.height - size) / 2
        val square = Bitmap.createBitmap(toTransform, x, y, size, size)
        var result = pool.get(size, size, Bitmap.Config.ARGB_8888)
        if (result == null) {
            result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        }
        //画图
        val canvas = Canvas(result)
        val paint = Paint()
        //设置 Shader
        paint.shader =
            BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true
        val radius = size / 2f
        //绘制一个圆
        canvas.drawCircle(radius, radius, radius, paint)

        return result
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray(CHARSET))
    }

    override fun equals(other: Any?): Boolean {
        return other is CircleTransform
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }


}