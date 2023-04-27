package com.takusemba.spotlight.shape

import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Px
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * [Shape] of Circle with customizable radius.
 */
class Circle(
    @Px private val radius: Float,
    duration: Duration = 500.milliseconds,
    interpolator: TimeInterpolator = DecelerateInterpolator(2f)
) : Shape(duration, interpolator) {
    private val latestDrawnRectF = RectF()

    override fun draw(canvas: Canvas, rectangle: Rect, animatedValue: Float, paint: Paint) {
        latestDrawnRectF.set(rectangle)
        canvas.drawCircle(
            rectangle.exactCenterX(),
            rectangle.exactCenterY(),
            animatedValue * radius,
            paint
        )
    }

    override fun contains(point: PointF): Boolean {
        val x = latestDrawnRectF.centerX()
        val y = latestDrawnRectF.centerY()
        val xNorm = point.x - x
        val yNorm = point.y - y
        return (xNorm * xNorm + yNorm * yNorm) <= radius * radius
    }
}
