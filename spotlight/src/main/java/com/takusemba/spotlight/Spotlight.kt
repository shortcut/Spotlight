package com.takusemba.spotlight

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.ColorInt
import androidx.annotation.TransitionRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.contains
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.takusemba.spotlight.extension.doOnceOnEnd
import com.takusemba.spotlight.extension.doOnceOnStart

/**
 * Holds all of the [Target]s and [SpotlightView] to show/hide [Target], [SpotlightView] properly.
 * [SpotlightView] can be controlled with [start]/[finish].
 * All of the [Target]s can be controlled with [next]/[previous]/[show].
 *
 * Once you finish the current [Spotlight] with [finish], you can not start the [Spotlight] again
 * unless you create a new [Spotlight] to start again.
 */
class Spotlight private constructor(
    private val spotlightView: SpotlightView,
    private val targets: Array<Target>,
    private val container: ViewGroup,
    private val spotlightListener: OnSpotlightListener?,
    finishOnTouchOutsideOfCurrentTarget: Boolean,
    finishOnBackPress: Boolean,
    enterTransition: Any?,
    exitTransition: Any?
) {
    var currentIndex = NO_POSITION
        private set

    private val transitionInflater by lazy(LazyThreadSafetyMode.NONE) {
        TransitionInflater.from(spotlightView.context)
    }

    private val enterTransition: Transition by lazy(LazyThreadSafetyMode.NONE) {
        enterTransition.asTransition { DEFAULT__SPOTLIGHT_TRANSITION.clone() }
    }

    private val exitTransition: Transition by lazy(LazyThreadSafetyMode.NONE) {
        exitTransition.asTransition { DEFAULT__SPOTLIGHT_TRANSITION.clone() }
    }

    private var pendingStart = false
    private var pendingFinish = false

    init {
        spotlightView.apply {
            if (finishOnTouchOutsideOfCurrentTarget) {
                setOnTouchOutsideOfCurrentTargetListener {
                    finishSpotlight()
                }
            }

            if (finishOnBackPress) {
                isFocusable = true
                isFocusableInTouchMode = true
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        finishSpotlight()
                        true
                    } else false
                }
            }
        }
    }

    /**
     * Starts [SpotlightView] and show the first [Target].
     */
    fun start() {
        startSpotlight()
    }

    /**
     * Closes the current [Target] if exists, and shows a [Target] at the specified [index].
     * If target is not found at the [index], it will throw an exception.
     */
    fun show(index: Int) {
        showTarget(index)
    }

    /**
     * Closes the current [Target] if exists, and shows the next [Target].
     * If the next [Target] is not found, Spotlight will finish.
     */
    fun next() {
        showTarget(currentIndex + 1)
    }

    /**
     * Closes the current [Target] if exists, and shows the previous [Target].
     * If the previous target is not found, it will throw an exception.
     */
    fun previous() {
        showTarget(currentIndex - 1)
    }

    /**
     * Closes Spotlight and [SpotlightView] will remove all children and be removed from the [container].
     */
    fun finish() {
        finishSpotlight()
    }

    /**
     * Invalidates the target's location which may be useful for instance when the target is
     * scrolled, and the overlay's position need to be refreshed.
     */
    fun invalidateTargetLocation() {
        spotlightView.invalidateTargetLocation()
    }

    /**
     * Starts Spotlight.
     */
    private fun startSpotlight() {
        if (pendingStart) return
        pendingStart = true

        ViewCompat.postOnAnimation(container) {
            enterTransition.doOnceOnStart {
                spotlightView.requestFocus()
                spotlightListener?.onStarted()
            }

            enterTransition.doOnceOnEnd {
                showTarget(0)
                pendingStart = false
            }

            if (spotlightView !in container) {
                TransitionManager.beginDelayedTransition(container, enterTransition)

                container.addView(spotlightView, MATCH_PARENT, MATCH_PARENT)
            }
        }
    }

    /**
     * Closes the current [Target] if exists, and show the [Target] at [index].
     */
    private fun showTarget(index: Int) {
        if (currentIndex == NO_POSITION) {
            enterTarget(index)
        } else {
            spotlightView.finishTarget {
                if (index <= NO_POSITION || index > targets.lastIndex) {
                    exitTarget(currentIndex) {
                        finishSpotlight()
                    }
                } else {
                    exitTarget(currentIndex) {
                        enterTarget(index)
                    }
                }
            }
        }
    }

    private fun enterTarget(index: Int) {
        val target = targets[index]
        val resolvedEnterTransition = target.enterTransition
            .asTransition { DEFAULT__TARGET_TRANSITION.clone() }
            .doOnceOnStart { target.listener?.onStarting(target, index) }
            .doOnceOnEnd {
                currentIndex = index
                target.listener?.onStarted(target, index)
            }

        spotlightView.setTarget(target, resolvedEnterTransition)
    }

    private fun exitTarget(index: Int, onEnd: () -> Unit) {
        val target = targets[index]

        val resolvedExitTransition = target.exitTransition
            .asTransition { DEFAULT__TARGET_TRANSITION.clone() }
            .doOnceOnEnd {
                target.listener?.onEnded(target, index)
                onEnd()
            }

        spotlightView.removeTarget(resolvedExitTransition)
    }

    /**
     * Closes Spotlight.
     */
    private fun finishSpotlight() {
        if (pendingFinish) return
        pendingFinish = true

        ViewCompat.postOnAnimation(container) {
            exitTransition.doOnceOnEnd {
                spotlightView.cleanup()
                spotlightListener?.onEnded()
                currentIndex = NO_POSITION
                pendingFinish = false
            }

            if (spotlightView in container) {
                TransitionManager.beginDelayedTransition(container, exitTransition)
                container.removeView(spotlightView)
            }
        }
    }

    private inline fun Any?.asTransition(default: () -> Transition): Transition {
        val receiver = this
        return if (receiver is Int && receiver != ResourcesCompat.ID_NULL) {
            transitionInflater.runCatching { inflateTransition(receiver) }.getOrElse { default() }
        } else if (receiver is Transition) {
            receiver
        } else {
            default()
        }
    }

    companion object {
        private val DEFAULT__SPOTLIGHT_TRANSITION = AutoTransition()
        private val DEFAULT__TARGET_TRANSITION = DEFAULT__SPOTLIGHT_TRANSITION

        const val NO_POSITION = -1
    }

    /**
     * Builder to build [Spotlight].
     * All parameters should be set in this [Builder].
     */
    class Builder(private val context: Context) {
        private var targets: Array<Target>? = null

        @ColorInt
        private var backgroundColor: Int = DEFAULT_OVERLAY_COLOR
        private var container: ViewGroup? = null
        private var listener: OnSpotlightListener? = null

        // Finish on touch outside of current target feature is disabled by default
        private var finishOnTouchOutsideOfCurrentTarget: Boolean = false
        private var finishOnBackPress: Boolean = false

        private var enterTransition: Any? = null
        private var exitTransition: Any? = null

        /**
         * Sets [Target]s to show on [Spotlight].
         */
        fun setTargets(vararg targets: Target): Builder = apply {
            require(targets.isNotEmpty()) { "targets should not be empty. " }
            this.targets = arrayOf(*targets)
        }

        /**
         * Sets [Target]s to show on [Spotlight].
         */
        fun setTargets(targets: List<Target>): Builder = apply {
            require(targets.isNotEmpty()) { "targets should not be empty. " }
            this.targets = targets.toTypedArray()
        }

        /**
         * Sets [backgroundColor] on [Spotlight].
         */
        fun setBackgroundColor(@ColorInt backgroundColor: Int): Builder = apply {
            this.backgroundColor = backgroundColor
        }

        /**
         * Sets [container] to hold [SpotlightView]. DecorView will be used if not specified.
         */
        fun setContainer(container: ViewGroup) = apply {
            this.container = container
        }

        /**
         * Sets [OnSpotlightListener] to notify the state of [Spotlight].
         */
        fun setOnSpotlightListener(listener: OnSpotlightListener): Builder = apply {
            this.listener = listener
        }

        /**
         * Sets [finishOnTouchOutsideOfCurrentTarget] flag
         * to enable/disable (true/false) finishing on touch outside feature.
         */
        fun setFinishOnTouchOutsideOfCurrentTarget(
            finishOnTouchOutsideOfCurrentTarget: Boolean
        ) = apply {
            this.finishOnTouchOutsideOfCurrentTarget = finishOnTouchOutsideOfCurrentTarget
        }

        fun setFinishOnBackPress(finishOnBackPress: Boolean) = apply {
            this.finishOnBackPress = finishOnBackPress
        }

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

        fun build(): Spotlight {
            val spotlightView = SpotlightView(context).apply {
                setBackgroundColor(backgroundColor)
            }
            val targets = requireNotNull(targets) { "targets should not be null. " }
            val container = container ?: tryGetActivity(context)!!.window.decorView as ViewGroup
            return Spotlight(
                spotlightView = spotlightView,
                targets = targets,
                container = container,
                spotlightListener = listener,
                finishOnTouchOutsideOfCurrentTarget,
                finishOnBackPress,
                enterTransition,
                exitTransition
            )
        }

        companion object {
            @ColorInt
            private val DEFAULT_OVERLAY_COLOR: Int = 0x6000000

            private fun tryGetActivity(context: Context): Activity? {
                var ctx = context
                while (ctx is ContextWrapper) {
                    if (ctx is Activity) {
                        return ctx
                    }

                    ctx = ctx.baseContext
                }
                return null
            }
        }
    }
}
