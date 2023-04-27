package com.takusemba.spotlight.effect

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.animation.LinearInterpolator

/**
 * Additional effect drawing in loop to Shape.
 */
interface Effect {

  /**
   * [duration] to draw Effect.
   */
  val duration: Long

  /**
   * [interpolator] to draw Effect.
   */
  val interpolator: TimeInterpolator

  /**
   * [repeatMode] to draw Effect.
   */
  val repeatMode: Int

  /**
   * Draw the Effect.
   *
   * @param value the animated value from 0 to 1 and this value is looped until Target finishes.
   */
  fun draw(canvas: Canvas, rectangle: Rect, value: Float, paint: Paint)

  class None : Effect {
    override val duration: Long = 0L
    override val interpolator: TimeInterpolator = LinearInterpolator()
    override val repeatMode: Int = ObjectAnimator.REVERSE
    override fun draw(canvas: Canvas, rectangle: Rect, value: Float, paint: Paint) = Unit
  }
}
