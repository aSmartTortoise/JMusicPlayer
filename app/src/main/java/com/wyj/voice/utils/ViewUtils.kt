package com.wyj.voice.utils

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AbsListView
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager

object ViewUtils {

    fun canViewScrollUp(view: View?, x: Float, y: Float, defaultValueForNull: Boolean): Boolean {
        return if (view == null || !contains(view, x, y)) {
            defaultValueForNull
        } else view.canScrollVertically(-1)
    }

    fun canViewScrollDown(view: View?, x: Float, y: Float, defaultValueForNull: Boolean): Boolean {
        return if (view == null || !contains(view, x, y)) {
            defaultValueForNull
        } else view.canScrollVertically(1)
    }

    fun canViewScrollRight(view: View?, x: Float, y: Float, defaultValueForNull: Boolean): Boolean {
        return if (view == null || !contains(view, x, y)) {
            defaultValueForNull
        } else view.canScrollHorizontally(-1)
    }

    fun canViewScrollLeft(view: View?, x: Float, y: Float, defaultValueForNull: Boolean): Boolean {
        return if (view == null || !contains(view, x, y)) {
            defaultValueForNull
        } else view.canScrollHorizontally(1)
    }

    fun findAllScrollViews(parent: ViewGroup): View? {
        for (i in 0 until parent.childCount) {
            var child = parent.getChildAt(i)
            if (child!!.visibility != View.VISIBLE) {
                continue
            }
            if (isScrollableView(child)) {
                return child
            }
            if (child is ViewGroup) {
                child = findAllScrollViews(child)
                if (child != null) {
                    return child
                }
            }
        }
        return null
    }

    fun isScrollableView(view: View?): Boolean {
        return (view is ScrollView
                || view is HorizontalScrollView
                || view is NestedScrollView
                || view is AbsListView
                || view is RecyclerView
                || view is ViewPager
                || view is WebView)
    }

    fun contains(view: View, x: Float, y: Float): Boolean {
        val localRect = Rect()
        view.getGlobalVisibleRect(localRect)
        return localRect.contains(x.toInt(), y.toInt())
    }
}