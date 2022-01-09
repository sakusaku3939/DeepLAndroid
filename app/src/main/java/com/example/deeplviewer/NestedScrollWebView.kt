package com.example.deeplviewer

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.webkit.WebView
import android.widget.OverScroller
import androidx.core.view.*
import kotlin.math.abs

/**
 * Based on https://gist.github.com/alexmiragall/0c4c7163f7a17938518ce9794c4a5236
 *
 * A WebView which implements Nested Scrolling and can handle scrolling of child elements inside the WebView correctly.
 * The code was optimized to work with bottom sheets, but it might work for other NestedScroll operations as well.
 *
 */
class NestedScrollWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) :
    WebView(context, attrs, defStyleAttr), NestedScrollingChild {

    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)

    private val nestedScrollingChildHelper: NestedScrollingChildHelper
    private var configuration: ViewConfiguration
    private var scroller: OverScroller

    private var lastMotionY = 0
    private var nestedYOffset = 0
    private var isBeingDragged = false
    private var isScrollingDown = false

    private var velocityTracker: VelocityTracker? = null
    private var activePointerId = INVALID_POINTER

    companion object {
        private const val INVALID_POINTER = -1
        private const val TAG = "NestedWebView"
    }

    init {
        overScrollMode = OVER_SCROLL_NEVER
        scroller = OverScroller(context)
        configuration = ViewConfiguration.get(context)
        nestedScrollingChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true
        }
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                activePointerId
                if (activePointerId == INVALID_POINTER) {
                    return isBeingDragged
                } else {
                    val pointerIndex = ev.findPointerIndex(activePointerId)
                    if (pointerIndex == -1) {
                        Log.e(
                            TAG, "Invalid pointerId=" + activePointerId
                                    + " in onInterceptTouchEvent"
                        )
                    } else {
                        val y = ev.getY(pointerIndex).toInt()
                        if (lastMotionY == 0) {
                            lastMotionY = y
                        }
                        val yDiff = y - lastMotionY
                        if (abs(yDiff) > configuration.scaledTouchSlop
                            && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL == 0
                        ) {
                            lastMotionY = y
                            initVelocityTrackerIfNotExists()
                            velocityTracker!!.addMovement(ev)
                            nestedYOffset = 0
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
            }
            MotionEvent.ACTION_DOWN -> {
                lastMotionY = ev.y.toInt()
                activePointerId = ev.getPointerId(0)
                initOrResetVelocityTracker()
                velocityTracker!!.addMovement(ev)
                scroller.computeScrollOffset()
                if (scroller.isFinished) {
                    isBeingDragged = false
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isBeingDragged = false
                activePointerId = INVALID_POINTER
                recycleVelocityTracker()
                if (scroller.springBack(scrollX, scrollY, 0, 0, 0, computeVerticalScrollRange())) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                stopNestedScroll()
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }
        return isBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()
        val vtev = MotionEvent.obtain(ev)
        val actionMasked = MotionEventCompat.getActionMasked(ev)
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            nestedYOffset = 0
        }
        vtev.offsetLocation(0f, nestedYOffset.toFloat())
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastMotionY = ev.y.toInt()
                activePointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = ev.findPointerIndex(activePointerId)
                if (activePointerIndex == -1) {
                    Log.e(
                        TAG,
                        "Invalid pointerId=$activePointerId in onTouchEvent"
                    )
                } else {
                    val y = ev.getY(activePointerIndex).toInt()
                    if (lastMotionY == 0) {
                        lastMotionY = y
                    }
                    var deltaY = lastMotionY - y
                    if (deltaY < -configuration.scaledTouchSlop) {
                        if (isScrollingDown) {
                            endNestedDrag()
                        }
                        isScrollingDown = false
                    }
                    if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
                        deltaY -= scrollConsumed[1]
                        vtev.offsetLocation(0f, scrollOffset[1].toFloat())
                        nestedYOffset += scrollOffset[1]
                    }
                    if (!isBeingDragged && abs(deltaY) > configuration.scaledTouchSlop) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        if (deltaY > 0) {
                            deltaY -= configuration.scaledTouchSlop
                            startNestedDrag()
                            isScrollingDown = true
                        } else {
                            deltaY += configuration.scaledTouchSlop
                        }
                    }
                    if (isBeingDragged) {
                        lastMotionY = y - scrollOffset[1]
                        val oldY = scrollY
                        val scrolledDeltaY = scrollY - oldY
                        val unconsumedY = deltaY - scrolledDeltaY
                        if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, scrollOffset)) {
                            lastMotionY -= scrollOffset[1]
                            vtev.offsetLocation(0f, scrollOffset[1].toFloat())
                            nestedYOffset += scrollOffset[1]
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isBeingDragged) {
                    velocityTracker!!.computeCurrentVelocity(
                        1000,
                        configuration.scaledMaximumFlingVelocity.toFloat()
                    )
                    val initialVelocity = VelocityTrackerCompat.getYVelocity(
                        velocityTracker,
                        activePointerId
                    ).toInt()
                    if (abs(initialVelocity) > configuration.scaledMinimumFlingVelocity) {
                        flingWithNestedDispatch(-initialVelocity)
                    } else if (scroller.springBack(
                            scrollX, scrollY, 0, 0, 0,
                            computeVerticalScrollRange()
                        )
                    ) {
                        ViewCompat.postInvalidateOnAnimation(this)
                    }
                }
                activePointerId = INVALID_POINTER
                endNestedDrag()
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged && childCount > 0) {
                    if (scroller.springBack(
                            scrollX, scrollY, 0, 0, 0,
                            computeVerticalScrollRange()
                        )
                    ) {
                        ViewCompat.postInvalidateOnAnimation(this)
                    }
                }
                activePointerId = INVALID_POINTER
                endNestedDrag()
            }
            MotionEventCompat.ACTION_POINTER_DOWN -> {
                val index = MotionEventCompat.getActionIndex(ev)
                lastMotionY = ev.getY(index).toInt()
                activePointerId = ev.getPointerId(index)
            }
            MotionEventCompat.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                lastMotionY = ev.getY(ev.findPointerIndex(activePointerId)).toInt()
            }
        }
        velocityTracker?.addMovement(vtev)
        vtev.recycle()
        return super.onTouchEvent(ev)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)

        if (clampedY && !isBeingDragged) {
            parent?.requestDisallowInterceptTouchEvent(true)
            initOrResetVelocityTracker()
            isScrollingDown = false
            lastMotionY = 0
            nestedYOffset = 0
            startNestedDrag()
        }
    }

    private fun startNestedDrag() {
        isBeingDragged = true
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
    }

    private fun endNestedDrag() {
        isBeingDragged = false
        recycleVelocityTracker()
        stopNestedScroll()
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = (ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK
                shr MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == activePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            lastMotionY = ev.getY(newPointerIndex).toInt()
            activePointerId = ev.getPointerId(newPointerIndex)
            velocityTracker?.clear()
        }
    }

    private fun initOrResetVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker!!.clear()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun flingWithNestedDispatch(velocityY: Int) {
        val scrollY = scrollY
        val canFling = ((scrollY > 0 || velocityY > 0)
                && (scrollY < computeVerticalScrollRange() || velocityY < 0))
        if (!dispatchNestedPreFling(0f, velocityY.toFloat())) {
            dispatchNestedFling(0f, velocityY.toFloat(), canFling)
            if (canFling) {
                fling(velocityY)
            }
        }
    }

    private fun fling(velocityY: Int) {
        if (childCount > 0) {
            val height = height - paddingBottom - paddingTop
            val bottom = getChildAt(0).height
            scroller.fling(
                scrollX, scrollY, 0, velocityY, 0, 0, 0,
                (bottom - height).coerceAtLeast(0), 0, height / 2
            )
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return nestedScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        nestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return nestedScrollingChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        nestedScrollingChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return nestedScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun getNestedScrollAxes(): Int {
        return ViewCompat.SCROLL_AXIS_NONE
    }
}
