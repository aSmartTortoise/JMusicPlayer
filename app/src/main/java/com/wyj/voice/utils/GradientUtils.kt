package com.wyj.voice.utils

import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

object GradientUtils {
    fun create(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int,
        radius: Int,
        @FloatRange(from = 0.0, to = 1.0) centerX: Float,
        @FloatRange(from = 0.0, to = 1.0) centerY: Float
    ): GradientDrawable {
        val colorArray = intArrayOf(startColor, endColor)
        return GradientDrawable().apply {
            colors = colorArray
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = radius.toFloat()
            setGradientCenter(centerX, centerY)
        }
    }
}