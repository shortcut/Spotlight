package com.takusemba.spotlight

import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.annotation.TransitionRes
import androidx.transition.Transition
import com.takusemba.spotlight.effect.Effect
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.Shape

/**
 * Target represents the spot that Spotlight will cast.
 */
class Target private constructor(
    internal val shape: Shape,
    internal val effect: Effect,
    internal val overlay: View,
    internal val enterTransition: Any?,
    internal val exitTransition: Any?,
    @Px internal val verticalOffset: Int,
    private val windowLocationProvider: () -> Rect,
    internal val listener: OnTargetListener?
) {
    val windowLocation: Rect
        get() = windowLocationProvider()

    /**
     * Checks if point on edge or inside of the Shape.
     *
     * @param point point to check against contains.
     * @return true if contains, false - otherwise.
     */
    operator fun contains(point: PointF): Boolean = shape.contains(point)

    /**
     * [Builder] to build a [Target].
     * All parameters should be set in this [Builder].
     */
    class Builder {
        private var deferredRectangle: (() -> Rect)? = null

        private var shape: Shape? = null
        private var effect: Effect? = null
        private lateinit var overlay: View
        private var listener: OnTargetListener? = null

        private var enterTransition: Any? = null
        private var exitTransition: Any? = null

        @Px
        private var verticalOffset: Int = 0

        fun setAnchor(deferredRectangle: () -> Rect) = apply {
            this.deferredRectangle = deferredRectangle
        }

        fun setAnchor(lazyView: Lazy<View>) = apply {
            setAnchor {
                val location = IntArray(2)
                with(lazyView.value) {
                    getLocationInWindow(location)
                    Rect(location[0], location[1], location[0] + width, location[1] + height)
                }
            }
        }

        /**
         * Sets a pointer to start a [Target].
         */
        fun setAnchor(view: View): Builder = apply {
            setAnchor(lazyOf(view))
        }

        fun setVerticalOffset(@Px verticalOffset: Int) =
            apply { this.verticalOffset = verticalOffset }

        /**
         * Sets [shape] of the spot of [Target].
         */
        fun setShape(shape: Shape): Builder = apply { this.shape = shape }

        /**
         * Sets [effect] of the spot of [Target].
         */
        fun setEffect(effect: Effect): Builder = apply { this.effect = effect }

        /**
         * Sets [overlay] to be laid out to describe [Target].
         */
        fun setOverlay(overlay: View): Builder = apply { this.overlay = overlay }

        fun setEnterTransition(enterTransition: Transition) = apply {
            this.enterTransition = enterTransition
        }

        fun setEnterTransition(@TransitionRes enterTransition: Int) = apply {
            this.enterTransition = enterTransition
        }

        fun setExitTransition(exitTransition: Transition) = apply {
            this.exitTransition = exitTransition
        }

        fun setExitTransition(@TransitionRes exitTransition: Int) = apply {
            this.exitTransition = exitTransition
        }

        /**
         * Sets [OnTargetListener] to notify the state of [Target].
         */
        fun setOnTargetListener(
            listener: OnTargetListener
        ): Builder = apply { this.listener = listener }

        fun build() = Target(
            windowLocationProvider = requireNotNull(deferredRectangle) {
                "Window location provider or anchor must be provided"
            },
            shape = shape ?: Circle(100f),
            effect = effect ?: Effect.None(),
            overlay = overlay,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            verticalOffset = verticalOffset,
            listener = listener
        )
    }
}
