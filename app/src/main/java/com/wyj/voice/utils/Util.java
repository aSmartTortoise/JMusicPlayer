package com.wyj.voice.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

/**
 * Created by GongWen on 17/8/25.
 */

public class Util {
    public static boolean canViewScrollUp(View view, float x, float y, boolean defaultValueForNull) {
        if (view == null || !contains(view, x, y)) {
            return defaultValueForNull;
        }
        return view.canScrollVertically(-1);
    }

    public static boolean canViewScrollDown(View view, float x, float y, boolean defaultValueForNull) {
        if (view == null || !contains(view, x, y)) {
            return defaultValueForNull;
        }
        return view.canScrollVertically(1);
    }

    public static boolean canViewScrollRight(View view, float x, float y, boolean defaultValueForNull) {
        if (view == null || !contains(view, x, y)) {
            return defaultValueForNull;
        }
        return view.canScrollHorizontally(-1);
    }

    public static boolean canViewScrollLeft(View view, float x, float y, boolean defaultValueForNull) {
        if (view == null || !contains(view, x, y)) {
            return defaultValueForNull;
        }
        return view.canScrollHorizontally(1);
    }


    public static View findAllScrollViews(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (isScrollableView(child)) {
                return child;
            }
            if (child instanceof ViewGroup) {
                child = findAllScrollViews((ViewGroup) child);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    public static boolean isScrollableView(View view) {
        return view instanceof ScrollView
                || view instanceof HorizontalScrollView
                || view instanceof NestedScrollView
                || view instanceof AbsListView
                || view instanceof RecyclerView
                || view instanceof ViewPager
                || view instanceof WebView;
    }

    public static boolean contains(View view, float x, float y) {
        Rect localRect = new Rect();
        view.getGlobalVisibleRect(localRect);
        return localRect.contains((int) x, (int) y);
    }

//    public static void onPanelSlide(float fraction) {
//        Activity activity = WxSwipeBackActivityManager.getInstance().getPenultimateActivity();
//        if (activity != null && !activity.isFinishing()) {
//            View decorView = activity.getWindow().getDecorView();
//            ViewCompat.setTranslationX(decorView, -(decorView.getMeasuredWidth() / 3.0f) * (1 - fraction));
//        }
//    }
//
//    public static void onPanelReset() {
//        Activity activity = WxSwipeBackActivityManager.getInstance().getPenultimateActivity();
//        if (activity != null) {
//            View decorView = activity.getWindow().getDecorView();
//            ViewCompat.setTranslationX(decorView, 0);
//        }
//    }
}
