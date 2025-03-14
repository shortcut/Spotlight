package com.takusemba.spotlight.effect

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import java.util.concurrent.TimeUnit

/**
 * Draws an ripple effects.
 */
class RippleEffect @JvmOverloads constructor(
    private val offset: Float,
    private val radius: Float,
    @ColorInt private val color: Int,
    override val duration: Long = TimeUnit.MILLISECONDS.toMillis(1000),
    override val interpolator: TimeInterpolator = DecelerateInterpolator(1f),
    override val repeatMode: Int = ObjectAnimator.REVERSE
) : Effect {

    init {
        require(offset < radius) { "holeRadius should be bigger than rippleRadius." }
    }

    override fun draw(canvas: Canvas, rectangle: Rect, value: Float, paint: Paint) {
        val radius = offset + ((radius - offset) * value)
        val alpha = (255 - value * 255).toInt()
        paint.color = color
        paint.alpha = alpha
        canvas.drawCircle(rectangle.exactCenterX(), rectangle.exactCenterY(), radius, paint)
    }
}
