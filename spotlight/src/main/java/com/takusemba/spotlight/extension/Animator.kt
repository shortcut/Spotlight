package com.takusemba.spotlight.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter

internal inline fun Animator.doOnceOnEnd(
    crossinline action: (animator: Animator) -> Unit
) = apply {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            animation.removeListener(this)
            action.invoke(animation)
        }
    })
}

internal inline fun Animator.doOnceOnStart(
    crossinline action: (animator: Animator) -> Unit
) = apply {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            animation.removeListener(this)
            action.invoke(animation)
        }
    })
}

internal inline fun Animator.doOnceOnCancel(
    crossinline action: (animator: Animator) -> Unit
) = apply {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationCancel(animation: Animator) {
            animation.removeListener(this)
            action.invoke(animation)
        }
    })
}
