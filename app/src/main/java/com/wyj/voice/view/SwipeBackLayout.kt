package com.wyj.voice.view

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.wyj.voice.R
import com.wyj.voice.utils.Util

class SwipeBackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        const val FROM_LEFT = 1 shl 0
        const val FROM_RIGHT = 1 shl 1
        const val FROM_TOP = 1 shl 2
        const val FROM_BOTTOM = 1 shl 3
        private const val TAG = "SwipeBackLayout"
    }

    private var mDragHelper: ViewDragHelper
    private var mTouchSlop = 0
    private var mDragContentView: View? = null
    private var innerScrollView: View? = null
    private var leftOffset = 0
    private var topOffset = 0
    var isSwipeFromEdge = false
    private var mDirectionMode = FROM_LEFT
    private var touchedEdge = ViewDragHelper.INVALID_POINTER
    private var downX = 0f
    private var downY = 0f
    private var swipeBackFraction = 0f
    private var swipeBackFactor = 0.5f
    var autoFinishedVelocityLimit = 2000f
    private var maskAlpha = 125
    private var widthSize = 0
    var heightSize = 0

    private var mSwipeBackListener: OnSwipeBackListener? = null

    private val defaultSwipeBackListener = object : OnSwipeBackListener {
        override fun onViewPositionChanged(
            view: View?,
            swipeBackFraction: Float,
            swipeBackFactor: Float
        ) {
            invalidate()
        }

        override fun onViewSwipeFinished(view: View?, isEnd: Boolean) {
            Log.d(TAG, "onViewSwipeFinished: wyj isEnd:$isEnd")
            if (isEnd) {
                finish()
            }
        }
    }

    @IntDef(*[FROM_LEFT, FROM_TOP, FROM_RIGHT, FROM_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    annotation class DirectionMode

    init {
        setWillNotDraw(false)
        mDragHelper = ViewDragHelper.create(this, 1f, DragHelperCallback()).apply {
            setEdgeTrackingEnabled(mDirectionMode)
        }
        mTouchSlop = mDragHelper.touchSlop
        setSwipeBackListener(defaultSwipeBackListener)
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeBackLayout)
        setDirectionMode(a.getInt(R.styleable.SwipeBackLayout_directionMode, mDirectionMode))
        setSwipeBackFactor(a.getFloat(R.styleable.SwipeBackLayout_swipeBackFactor, swipeBackFactor))
        setMaskAlpha(a.getInteger(R.styleable.SwipeBackLayout_maskAlpha, maskAlpha))
        isSwipeFromEdge = a.getBoolean(R.styleable.SwipeBackLayout_isSwipeFromEdge, isSwipeFromEdge)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(TAG, "onMeasure: wyj")
        val childCount = childCount
        check(childCount <= 1) { "SwipeBackLayout must contains only one direct child." }
        var defaultMeasuredWidth = 0
        var defaultMeasuredHeight = 0
        if (childCount > 0) {
            measureChildren(widthMeasureSpec, heightMeasureSpec)
            mDragContentView = getChildAt(0)
            defaultMeasuredWidth = mDragContentView?.measuredWidth ?:0
            defaultMeasuredHeight = mDragContentView?.measuredHeight ?: 0
        }
        val measuredWidth: Int = resolveSize(defaultMeasuredWidth, widthMeasureSpec) + paddingLeft + paddingRight
        val measuredHeight: Int = resolveSize(defaultMeasuredHeight, heightMeasureSpec) + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.d(TAG, "onLayout: wyj changed:$changed")
        if (childCount == 0) return
        val left = paddingLeft + leftOffset
        val top = paddingTop + topOffset
        val right = left + mDragContentView!!.measuredWidth
        val bottom = top + mDragContentView!!.measuredHeight
        mDragContentView!!.layout(left, top, right, bottom)

        if (changed) {
            widthSize = width
            heightSize = height
        }
        innerScrollView = Util.findAllScrollViews(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw: wyj swipeBackFraction:$swipeBackFraction")
        canvas.drawARGB(maskAlpha - (maskAlpha * swipeBackFraction).toInt(), 0, 0, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        Log.d(TAG, "onInterceptTouchEvent: wyj")
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> if (innerScrollView != null && Util.contains(
                    innerScrollView,
                    downX,
                    downY
                )
            ) {
                val distanceX = Math.abs(ev.rawX - downX)
                val distanceY = Math.abs(ev.rawY - downY)
                if (mDirectionMode == FROM_LEFT || mDirectionMode == FROM_RIGHT) {
                    if (distanceY > mTouchSlop && distanceY > distanceX) {
                        return super.onInterceptTouchEvent(ev)
                    }
                } else if (mDirectionMode == FROM_TOP || mDirectionMode == FROM_BOTTOM) {
                    if (distanceX > mTouchSlop && distanceX > distanceY) {
                        return super.onInterceptTouchEvent(ev)
                    }
                }
            }
        }
        val handled = mDragHelper.shouldInterceptTouchEvent(ev)
        return if (handled) true else super.onInterceptTouchEvent(ev)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "onTouchEvent: wyj")
        mDragHelper.processTouchEvent(event!!)
        return true
    }

    override fun computeScroll() {
        Log.d(TAG, "computeScroll: wyj")
        if (mDragHelper.continueSettling(true)) {
            Log.d(TAG, "computeScroll: wyj is continue settling")
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun smoothScrollToX(finalLeft: Int) {
        if (mDragHelper.settleCapturedViewAt(finalLeft, paddingTop)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun smoothScrollToY(finalTop: Int) {
        if (mDragHelper.settleCapturedViewAt(paddingLeft, finalTop)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun setDirectionMode(@DirectionMode direction: Int) {
        mDirectionMode = direction
        mDragHelper.setEdgeTrackingEnabled(direction)
    }

    fun getDirectionMode(): Int {
        return mDirectionMode
    }

    private fun isSwipeEnabled(): Boolean {
        if (isSwipeFromEdge) {
            when (mDirectionMode) {
                FROM_LEFT -> return touchedEdge == ViewDragHelper.EDGE_LEFT
                FROM_TOP -> return touchedEdge == ViewDragHelper.EDGE_TOP
                FROM_RIGHT -> return touchedEdge == ViewDragHelper.EDGE_RIGHT
                FROM_BOTTOM -> return touchedEdge == ViewDragHelper.EDGE_BOTTOM
            }
        }
        return true
    }

    fun setSwipeBackFactor(@FloatRange(from = 0.0, to = 1.0) swipeBackFactor: Float) {
        var swipeBackFactor = swipeBackFactor
        if (swipeBackFactor > 1) {
            swipeBackFactor = 1f
        } else if (swipeBackFactor < 0) {
            swipeBackFactor = 0f
        }
        this.swipeBackFactor = swipeBackFactor
    }

    fun setMaskAlpha(@androidx.annotation.IntRange(from = 0, to = 255) maskAlpha: Int) {
        var maskAlpha = maskAlpha
        if (maskAlpha > 255) {
            maskAlpha = 255
        } else if (maskAlpha < 0) {
            maskAlpha = 0
        }
        this.maskAlpha = maskAlpha
    }

    private fun backJudgeBySpeed(xvel: Float, yvel: Float): Boolean {
        when (mDirectionMode) {
            FROM_LEFT -> return xvel > autoFinishedVelocityLimit
            FROM_TOP -> return yvel > autoFinishedVelocityLimit
            FROM_RIGHT -> return xvel < -autoFinishedVelocityLimit
            FROM_BOTTOM -> return yvel < -autoFinishedVelocityLimit
        }
        return false
    }

    fun finish() {
        (context as Activity).finish()
    }

    private inner class DragHelperCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            Log.d(TAG, "tryCaptureView: ")
            return child === mDragContentView
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            Log.d(TAG, "onViewCaptured: wyj activePointerId:$activePointerId")
            super.onViewCaptured(capturedChild, activePointerId)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            Log.d(TAG, "clampViewPositionHorizontal: wyj")
            leftOffset = paddingLeft
            if (isSwipeEnabled()) {
                if (mDirectionMode == FROM_LEFT && !Util.canViewScrollRight(
                        innerScrollView,
                        downX,
                        downY,
                        false
                    )
                ) {
                    leftOffset = Math.min(Math.max(left, paddingLeft), widthSize)
                } else if (mDirectionMode == FROM_RIGHT && !Util.canViewScrollLeft(
                        innerScrollView,
                        downX,
                        downY,
                        false
                    )
                ) {
                    leftOffset = Math.min(Math.max(left, -widthSize), paddingRight)
                }
            }
            return leftOffset
        }



        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            Log.d(TAG, "clampViewPositionVertical: wyj")
            topOffset = paddingTop
            if (isSwipeEnabled()) {
                if (mDirectionMode == FROM_TOP && !Util.canViewScrollUp(
                        innerScrollView,
                        downX,
                        downY,
                        false
                    )
                ) {
                    topOffset = Math.min(Math.max(top, paddingTop), heightSize)
                } else if (mDirectionMode == FROM_BOTTOM && !Util.canViewScrollDown(
                        innerScrollView,
                        downX,
                        downY,
                        false
                    )
                ) {
                    topOffset = Math.min(Math.max(top, -heightSize), paddingBottom)
                }
            }
            return topOffset
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            Log.d(TAG, "onViewPositionChanged: wyj left:$left, top:$top, dx:$dx, dy:$dy")
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            var left = left
            var top = top
            left = Math.abs(left)
            top = Math.abs(top)
            when (mDirectionMode) {
                FROM_LEFT, FROM_RIGHT -> swipeBackFraction =
                    1.0f * left / widthSize
                FROM_TOP, FROM_BOTTOM -> swipeBackFraction =
                    1.0f * top / heightSize
            }
            mSwipeBackListener?.onViewPositionChanged(
                mDragContentView,
                swipeBackFraction,
                swipeBackFactor
            )
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            Log.d(TAG, "onViewReleased: wyj")
            topOffset = 0
            leftOffset = topOffset
            if (!isSwipeEnabled()) {
                touchedEdge = ViewDragHelper.INVALID_POINTER
                return
            }
            touchedEdge = ViewDragHelper.INVALID_POINTER
            val isBackToEnd = backJudgeBySpeed(xvel, yvel) || swipeBackFraction >= swipeBackFactor
            if (isBackToEnd) {
                when (mDirectionMode) {
                    FROM_LEFT -> smoothScrollToX(widthSize)
                    FROM_TOP -> smoothScrollToY(heightSize)
                    FROM_RIGHT -> smoothScrollToX(-widthSize)
                    FROM_BOTTOM -> smoothScrollToY(-heightSize)
                }
            } else {
                when (mDirectionMode) {
                    FROM_LEFT, FROM_RIGHT -> smoothScrollToX(
                        paddingLeft
                    )
                    FROM_BOTTOM, FROM_TOP -> smoothScrollToY(
                        paddingTop
                    )
                }
            }
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            Log.d(TAG, "onViewDragStateChanged: wyj state:$state ,swipeBackFraction:$swipeBackFraction")
            if (state == ViewDragHelper.STATE_IDLE) {
                if (mSwipeBackListener != null) {
                    if (swipeBackFraction == 0f) {
                        mSwipeBackListener?.onViewSwipeFinished(mDragContentView, false)
                    } else if (swipeBackFraction == 1f) {
                        mSwipeBackListener?.onViewSwipeFinished(mDragContentView, true)
                    }
                }
            }
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return widthSize
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return heightSize
        }

        override fun onEdgeTouched(edgeFlags: Int, pointerId: Int) {
            super.onEdgeTouched(edgeFlags, pointerId)
            Log.d(TAG, "onEdgeTouched: wyj")
            touchedEdge = edgeFlags
        }
    }

    fun setSwipeBackListener(swipeBackListener: OnSwipeBackListener?) {
        this.mSwipeBackListener = swipeBackListener
    }

    interface OnSwipeBackListener {
        fun onViewPositionChanged(view: View?, swipeBackFraction: Float, swipeBackFactor: Float)
        fun onViewSwipeFinished(view: View?, isEnd: Boolean)
    }
}