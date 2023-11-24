package com.wyj.voice.ui.view

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

/**
 *  https://juejin.cn/post/6844903448622661645?from=search-suggest
 *  关于ViewDragHelper在自定义View中的应用。
 */
class SwipeBackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SwipeBackLayout"
    }

    private var viewDragHelper: ViewDragHelper
    private var dragContentView: View? = null
    private var innerScrollView: View? = null
    private var leftOffset = 0
    private var topOffset = 0
    var isSwipeFromEdge = false
    private var directionMode = ViewDragHelper.EDGE_LEFT
    private var touchedEdge = ViewDragHelper.INVALID_POINTER
    private var downX = 0f
    private var downY = 0f
    private var swipeBackFraction = 0f
    private var swipeBackFactor = 0.5f
    var autoFinishedVelocityLimit = 2000f
    private var maskAlpha = 125
    private var widthSize = 0
    private var heightSize = 0

    private var swipeBackListener: OnSwipeBackListener? = null

    private val defaultSwipeBackListener = object : OnSwipeBackListener {
        override fun onViewPositionChanged(
            view: View,
            swipeBackFraction: Float,
            swipeBackFactor: Float
        ) {
            invalidate()
        }

        override fun onViewSwipeFinished(view: View, isEnd: Boolean) {
            Log.d(TAG, "onViewSwipeFinished: wyj isEnd:$isEnd")
            if (isEnd) {
                finish()
            }
        }
    }

    @IntDef(*[ViewDragHelper.EDGE_LEFT, ViewDragHelper.EDGE_RIGHT, ViewDragHelper.EDGE_TOP, ViewDragHelper.EDGE_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    annotation class DirectionMode

    init {
        setWillNotDraw(false)
        viewDragHelper = ViewDragHelper.create(this, 1f, DragHelperCallback()).apply {
            setEdgeTrackingEnabled(directionMode)
        }
        setSwipeBackListener(defaultSwipeBackListener)
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeBackLayout)
        setDirectionMode(a.getInt(R.styleable.SwipeBackLayout_directionMode, directionMode))
        setSwipeBackFactor(a.getFloat(R.styleable.SwipeBackLayout_swipeBackFactor, swipeBackFactor))
        setMaskAlpha(a.getInteger(R.styleable.SwipeBackLayout_maskAlpha, maskAlpha))
        isSwipeFromEdge = a.getBoolean(R.styleable.SwipeBackLayout_isSwipeFromEdge, isSwipeFromEdge)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val childCount = childCount
        check(childCount <= 1) { "SwipeBackLayout must contains only one direct child." }
        var defaultMeasuredWidth = 0
        var defaultMeasuredHeight = 0
        if (childCount > 0) {
            measureChildren(widthMeasureSpec, heightMeasureSpec)
            dragContentView = getChildAt(0)
            defaultMeasuredWidth = dragContentView?.measuredWidth ?:0
            defaultMeasuredHeight = dragContentView?.measuredHeight ?: 0
        }
        val measuredWidth: Int = resolveSize(defaultMeasuredWidth, widthMeasureSpec) + paddingLeft + paddingRight
        val measuredHeight: Int = resolveSize(defaultMeasuredHeight, heightMeasureSpec) + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount == 0) return
        val left = paddingLeft + leftOffset
        val top = paddingTop + topOffset
        val right = left + dragContentView!!.measuredWidth
        val bottom = top + dragContentView!!.measuredHeight
        dragContentView!!.layout(left, top, right, bottom)

        if (changed) {
            widthSize = width
            heightSize = height
        }
        innerScrollView = Util.findAllScrollViews(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawARGB(maskAlpha - (maskAlpha * swipeBackFraction).toInt(), 0, 0, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        Log.d(TAG, "onInterceptTouchEvent: wyj")
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (innerScrollView != null && Util.contains(
                        innerScrollView,
                        downX,
                        downY
                    )
                ) {
                    val touchSlop = viewDragHelper.touchSlop
                    val distanceX = Math.abs(ev.rawX - downX)
                    val distanceY = Math.abs(ev.rawY - downY)
                    if (directionMode == ViewDragHelper.EDGE_LEFT
                        || directionMode == ViewDragHelper.EDGE_RIGHT
                    ) {
                        if (distanceY > touchSlop && distanceY > distanceX) {
                            return super.onInterceptTouchEvent(ev)
                        }
                    } else if (directionMode == ViewDragHelper.EDGE_TOP
                        || directionMode == ViewDragHelper.EDGE_BOTTOM
                    ) {
                        if (distanceX > touchSlop && distanceX > distanceY) {
                            return super.onInterceptTouchEvent(ev)
                        }
                    }
                }
            }
        }
        val handled = viewDragHelper.shouldInterceptTouchEvent(ev)
        return if (handled) true else super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var eventType = ""
        when(event.action) {
            MotionEvent.ACTION_DOWN -> eventType = "DOWN 事件"
            MotionEvent.ACTION_MOVE -> eventType = "MOVE 事件"
            MotionEvent.ACTION_UP -> eventType = "UP 事件"
            MotionEvent.ACTION_CANCEL -> eventType = "CANCEL 事件"
        }
        Log.d(TAG, "onTouchEvent: wyj eventType：$eventType")
        viewDragHelper.processTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun smoothScrollToX(finalLeft: Int) {
        if (viewDragHelper.settleCapturedViewAt(finalLeft, paddingTop)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun smoothScrollToY(finalTop: Int) {
        if (viewDragHelper.settleCapturedViewAt(paddingLeft, finalTop)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun setDirectionMode(@DirectionMode direction: Int) {
        directionMode = direction
        viewDragHelper.setEdgeTrackingEnabled(direction)
    }

    fun getDirectionMode() = directionMode

    private fun isSwipeEnabled(): Boolean {
        if (isSwipeFromEdge) {
            when (directionMode) {
                ViewDragHelper.EDGE_LEFT -> return touchedEdge == ViewDragHelper.EDGE_LEFT
                ViewDragHelper.EDGE_TOP -> return touchedEdge == ViewDragHelper.EDGE_TOP
                ViewDragHelper.EDGE_RIGHT -> return touchedEdge == ViewDragHelper.EDGE_RIGHT
                ViewDragHelper.EDGE_BOTTOM -> return touchedEdge == ViewDragHelper.EDGE_BOTTOM
            }
        }
        return true
    }

    fun setSwipeBackFactor(@FloatRange(from = 0.0, to = 1.0) swipeBackFactor: Float) {
        this.swipeBackFactor = swipeBackFactor
    }

    fun setMaskAlpha(@androidx.annotation.IntRange(from = 0, to = 255) maskAlpha: Int) {
        this.maskAlpha = maskAlpha
    }

    private fun backJudgeBySpeed(xvel: Float, yvel: Float): Boolean {
        when (directionMode) {
            ViewDragHelper.EDGE_LEFT -> return xvel > autoFinishedVelocityLimit
            ViewDragHelper.EDGE_TOP -> return yvel > autoFinishedVelocityLimit
            ViewDragHelper.EDGE_RIGHT -> return xvel < -autoFinishedVelocityLimit
            ViewDragHelper.EDGE_BOTTOM -> return yvel < -autoFinishedVelocityLimit
        }
        return false
    }

    fun finish() {
        (context as Activity).finish()
    }

    private inner class DragHelperCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            Log.d(TAG, "tryCaptureView: ")
            return child === dragContentView
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            Log.d(TAG, "onViewCaptured: wyj activePointerId:$activePointerId")
            super.onViewCaptured(capturedChild, activePointerId)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            Log.d(TAG, "clampViewPositionHorizontal: wyj")
            leftOffset = paddingLeft
            if (isSwipeEnabled()) {
                if (directionMode == ViewDragHelper.EDGE_LEFT && !Util.canViewScrollRight(
                        innerScrollView,
                        downX,
                        downY,
                        false
                    )
                ) {
                    leftOffset = Math.min(Math.max(left, paddingLeft), widthSize)
                } else if (directionMode == ViewDragHelper.EDGE_RIGHT && !Util.canViewScrollLeft(
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
                if (directionMode == ViewDragHelper.EDGE_TOP
                    && !Util.canViewScrollUp(innerScrollView, downX, downY, false)) {
                    topOffset = Math.min(Math.max(top, paddingTop), heightSize)
                } else if (directionMode == ViewDragHelper.EDGE_BOTTOM
                    && !Util.canViewScrollDown(innerScrollView, downX, downY, false)) {
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
            when (directionMode) {
                ViewDragHelper.EDGE_LEFT, ViewDragHelper.EDGE_RIGHT -> swipeBackFraction =
                    1.0f * left / widthSize
                ViewDragHelper.EDGE_TOP, ViewDragHelper.EDGE_BOTTOM -> swipeBackFraction =
                    1.0f * top / heightSize
            }
            swipeBackListener?.onViewPositionChanged(
                dragContentView!!,
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
            Log.d(TAG, "onViewReleased: wyj isBackToEnd:$isBackToEnd")
            if (isBackToEnd) {
                when (directionMode) {
                    ViewDragHelper.EDGE_LEFT -> smoothScrollToX(widthSize)
                    ViewDragHelper.EDGE_TOP -> smoothScrollToY(heightSize)
                    ViewDragHelper.EDGE_RIGHT -> smoothScrollToX(-widthSize)
                    ViewDragHelper.EDGE_BOTTOM -> smoothScrollToY(-heightSize)
                }
            } else {
                when (directionMode) {
                    ViewDragHelper.EDGE_LEFT, ViewDragHelper.EDGE_RIGHT -> smoothScrollToX(
                        paddingLeft
                    )
                    ViewDragHelper.EDGE_TOP, ViewDragHelper.EDGE_BOTTOM -> smoothScrollToY(
                        paddingTop
                    )
                }
            }
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            var stateEvent = ""
            when(state) {
                ViewDragHelper.STATE_IDLE -> stateEvent = "IDLE state"
                ViewDragHelper.STATE_DRAGGING -> stateEvent = "DRAGGING state"
                ViewDragHelper.STATE_SETTLING -> stateEvent = "SETTLING state"
            }
            Log.d(TAG, "onViewDragStateChanged: wyj stateEvent:$stateEvent ,swipeBackFraction:$swipeBackFraction")
            if (state == ViewDragHelper.STATE_IDLE) {
                swipeBackListener?.let {
                    if (swipeBackFraction == 0f) {
                        it.onViewSwipeFinished(dragContentView!!, false)
                    } else if (swipeBackFraction == 1f) {
                        it.onViewSwipeFinished(dragContentView!!, true)
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

    fun setSwipeBackListener(swipeBackListener: OnSwipeBackListener) {
        this.swipeBackListener = swipeBackListener
    }

    interface OnSwipeBackListener {
        fun onViewPositionChanged(view: View, swipeBackFraction: Float, swipeBackFactor: Float)
        fun onViewSwipeFinished(view: View, isEnd: Boolean)
    }
}