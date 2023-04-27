package com.takusemba.spotlight.shape

import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Px
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * [Shape] of RoundedRectangle which by default is as large as the spotlight.
 *
 * @param radius Radius of the corners
 * @param padding Padding around the spotlight
 */
class RoundedRectangle(
    @Px private val radius: Float,
    @Px private val padding: Float = 0f,
    duration: Duration = 500.milliseconds,
    interpolator: TimeInterpolator = DecelerateInterpolator(2f)
) : Shape(duration, interpolator) {
    private val latestDrawnRectF = RectF()

    override fun draw(canvas: Canvas, rectangle: Rect, animatedValue: Float, paint: Paint) {
        latestDrawnRectF.apply {
            set(rectangle)
            inset(-padding, -padding)

            val halfWidth = width() / 2 * animatedValue
            val halfHeight = height() / 2 * animatedValue
            val x = centerX()
            val y = centerY()
            set(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight)
        }

        canvas.drawRoundRect(latestDrawnRectF, radius, radius, paint)
    }

    override fun contains(point: PointF): Boolean {
        val x = latestDrawnRectF.centerX()
        val y = latestDrawnRectF.centerY()
        val xNorm = point.x - x
        val yNorm = point.y - y
        val widthHalf = latestDrawnRectF.width() / 2
        val heightHalf = latestDrawnRectF.height() / 2
        val r = radius.coerceIn(minimumValue = 0f, maximumValue = widthHalf)
        val n = maxOf(latestDrawnRectF.width(), latestDrawnRectF.height()) / r
        return abs((xNorm / widthHalf)).pow(n) + abs((yNorm / heightHalf)).pow(n) <= 1
    }
}

