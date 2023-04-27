@file:Suppress("DEPRECATION")

package com.takusemba.spotlight

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.ofFloat
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AbsoluteLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.takusemba.spotlight.extension.doOnceOnCancel
import com.takusemba.spotlight.extension.doOnceOnEnd

/**
 * [SpotlightView] starts/finishes [Spotlight], and starts/finishes a current [Target].
 */
internal class SpotlightView(
    context: Context,
) : AbsoluteLayout(context, null, ResourcesCompat.ID_NULL) {
    private val offsetBuffer = IntArray(2)

    private val shapePaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    }

    private val effectPaint by lazy(LazyThreadSafetyMode.NONE, ::Paint)

    private val invalidator = AnimatorUpdateListener { invalidate() }

    private var shapeAnimator: ValueAnimator? = null
    private var effectAnimator: ValueAnimator? = null
    private var target: Target? = null

    private var onTouchOutsideOfCurrentTargetListener: (() -> Unit)? = null

    private var downTouch = false

    private var touchPoint: PointF? = null

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentTarget = target
        val currentShapeAnimator = shapeAnimator
        val currentEffectAnimator = effectAnimator
        val localLocation = currentTarget?.getLocalLocation() ?: return
        if (currentEffectAnimator != null) {
            currentTarget.effect.draw(
                canvas = canvas,
                rectangle = localLocation,
                value = currentEffectAnimator.animatedValue as Float,
                paint = effectPaint
            )
        }
        if (currentShapeAnimator != null) {
            currentTarget.shape.draw(
                canvas = canvas,
                rectangle = localLocation,
                animatedValue = currentShapeAnimator.animatedValue as Float,
                paint = shapePaint
            )
        }
    }

    /**
     * Based on guide:
     * https://developer.android.com/guide/topics/ui/accessibility/custom-views#custom-click-events
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTouch = true

                true // Prevents click-through. TODO: Do we need to support click-through?
            }

            MotionEvent.ACTION_UP -> if (downTouch) {
                downTouch = false
                touchPoint = PointF(event.x, event.y)
                performClick() // Call this method to handle the response, and
                // thereby enable accessibility services to
                // perform this action for a user who cannot
                // click the touchscreen.
                true
            } else {
                false
            }

            else -> false
        }
    }

    override fun performClick(): Boolean {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the onClick() listener on the view, if any
        super.performClick()

        touchPoint = touchPoint?.run {
            val currentTarget = target ?: return@run null
            onTouchOutsideOfCurrentTargetListener?.also { listener ->
                if (this !in currentTarget) {
                    listener.invoke()
                }
            }
            null
        }

        return true
    }

    fun setTarget(target: Target, targetEnterTransition: Transition) {
        this.target = target

        val localLocation = target.getLocalLocation()
        val childLayoutParams = target.overlay.layoutParams?.let { source ->
            if (source is LayoutParams) {
                source.x = 0
                source.y = localLocation.bottom + target.verticalOffset
                source
            } else {
                LayoutParams(
                    source.width, source.height, 0,
                    localLocation.bottom + target.verticalOffset
                )
            }
        } ?: LayoutParams(
            MATCH_PARENT, WRAP_CONTENT, 0,
            localLocation.bottom + target.verticalOffset
        )

        TransitionManager.beginDelayedTransition(this, targetEnterTransition)
        addView(target.overlay, childLayoutParams)

        shapeAnimator = shapeAnimator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }.run {
            ofFloat(0f, 1f).apply {
                duration = target.shape.duration.inWholeMilliseconds
                interpolator = target.shape.interpolator
                addUpdateListener(invalidator)
                doOnceOnEnd { removeAllUpdateListeners() }
                doOnceOnCancel { removeAllUpdateListeners() }
            }
        }.also(ValueAnimator::start)

        effectAnimator = effectAnimator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }.run {
            ofFloat(0f, 1f).apply {
                startDelay = target.shape.duration.inWholeMilliseconds
                duration = target.effect.duration
                interpolator = target.effect.interpolator
                repeatMode = target.effect.repeatMode
                repeatCount = INFINITE
                addUpdateListener(invalidator)
                doOnceOnEnd { removeAllUpdateListeners() }
                doOnceOnCancel { removeAllUpdateListeners() }
            }
        }.also(ValueAnimator::start)
    }

    fun removeTarget(exitTransition: Transition) {
        TransitionManager.beginDelayedTransition(this, exitTransition)
        removeAllViews()
    }

    /**
     * Finishes the current [Target].
     */
    fun finishTarget(onEnd: () -> Unit) {
        val currentTarget = target ?: return
        val currentAnimatedValue = shapeAnimator?.animatedValue ?: return

        shapeAnimator = shapeAnimator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }.run {
            ofFloat(currentAnimatedValue as Float, 0f).apply {
                duration = currentTarget.shape.duration.inWholeMilliseconds
                interpolator = currentTarget.shape.interpolator
                addUpdateListener(invalidator)
                doOnceOnEnd { onEnd.invoke() }
                doOnceOnEnd { removeAllUpdateListeners() }
                doOnceOnCancel { removeAllUpdateListeners() }
            }
        }.also(ValueAnimator::start)

        effectAnimator = effectAnimator?.run {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
            null
        }
    }

    fun cleanup() {
        effectAnimator = effectAnimator?.run {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
            null
        }

        shapeAnimator = shapeAnimator?.run {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
            null
        }

        removeAllViews()
    }

    fun invalidateTargetLocation() {
        val currentTarget = target ?: return
        val localLocation = currentTarget.getLocalLocation()

        currentTarget.overlay.updateLayoutParams<LayoutParams> {
            y = localLocation.bottom + currentTarget.verticalOffset
        }
    }

    fun setOnTouchOutsideOfCurrentTargetListener(listener: () -> Unit) {
        onTouchOutsideOfCurrentTargetListener = listener
    }

    private fun Target.getLocalLocation(): Rect {
        // adjust anchor in case where custom container is set.
        getLocationInWindow(offsetBuffer)

        return windowLocation.apply { offset(-offsetBuffer[0], -offsetBuffer[1]) }
    }
}
