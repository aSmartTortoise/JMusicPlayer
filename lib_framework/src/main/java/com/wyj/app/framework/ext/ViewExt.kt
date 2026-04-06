package com.wyj.app.framework.ext

import android.graphics.Rect
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi

/**
 *  author : jie wang
 *  date : 2024/4/3 18:07
 *  description :
 */

/**
 * 获取字体绘制一行高度
 * @return Float
 */
fun TextView.getScentHeight(): Float {
    val fm = paint.fontMetrics
    return fm.descent - fm.ascent + fm.leading
}

/**
 * 获取字体绘制一行高度
 * @return Float
 */
fun TextView.getHeightFontMetrics(): Float {
    val fm = paint.fontMetrics
    return paint.getFontMetrics(fm)
}

/**
 * 获取字体一行高度(包含文本上下间距)
 * @return Float
 */
fun TextView.getHeightWithFontPadding(): Float {
    val fm = paint.fontMetrics
    return fm.bottom - fm.top + fm.leading
}

/**
 * 获取字体一行高度
 * @return Int
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun TextView.getTextBoundsHeight(): Int {
    val rect = Rect()
    paint.getTextBounds(text, 0, text.length, rect)
    return rect.height()
}

/**
 * 获取字体一行高度(包含文本上下间距)
 * @return Int
 */
fun TextView.getHeightFontMetricsInt(): Int {
    val fontMetricsInt = paint.fontMetricsInt
    return fontMetricsInt.bottom - fontMetricsInt.top
}
