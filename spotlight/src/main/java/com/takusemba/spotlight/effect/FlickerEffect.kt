package com.takusemba.spotlight.effect

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import java.util.concurrent.TimeUnit

/**
 * Draws an flicker effects.
 */
class FlickerEffect @JvmOverloads constructor(
    private val radius: Float,
    @ColorInt private val color: Int,
    override val duration: Long = TimeUnit.MILLISECONDS.toMillis(1000),
    override val interpolator: TimeInterpolator = LinearInterpolator(),
    override val repeatMode: Int = ObjectAnimator.REVERSE
) : Effect {

  override fun draw(canvas: Canvas, rectangle: Rect, value: Float, paint: Paint) {
    paint.color = color
    paint.alpha = (value * 255).toInt()
    canvas.drawCircle(rectangle.exactCenterX(), rectangle.exactCenterY(), radius, paint)
  }
}
