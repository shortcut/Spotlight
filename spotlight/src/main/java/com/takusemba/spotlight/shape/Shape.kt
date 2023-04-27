package com.takusemba.spotlight.shape

import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.takusemba.spotlight.Target
import kotlin.time.Duration

/**
 * Shape of a [Target] that would be drawn by Spotlight View.
 * For any shape of target, this Shape class need to be implemented.
 */
open class Shape(internal val duration: Duration, internal val interpolator: TimeInterpolator) {
    private val latestDrawnRectF = RectF()

    /**
     * Draws the Shape.
     *
     * @param animatedValue the animated value from 0 to 1.
     */
    open fun draw(canvas: Canvas, rectangle: Rect, animatedValue: Float, paint: Paint) {
        latestDrawnRectF.set(rectangle)
        canvas.drawRect(latestDrawnRectF, paint)
    }

    /**
     * Checks whether this point is within the Shape's touch zone.
     *
     * The default implementation relies on the Rectangle provided in draw()
     *
     * @param point point to check against contains.
     * @return true if contains, false - otherwise.
     */
    open operator fun contains(point: PointF): Boolean {
        return latestDrawnRectF.contains(point.x, point.y)
    }
}
