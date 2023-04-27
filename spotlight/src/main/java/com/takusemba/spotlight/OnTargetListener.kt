package com.takusemba.spotlight

/**
 * Listener to notify the state of Target.
 */
interface OnTargetListener {
    /**
     * Called when Target is about to start which once its enter transition has started.
     */
    fun onStarting(target: Target, index: Int) {}

    /**
     * Called when Target is started which is once its enter transition has ended.
     */
    fun onStarted(target: Target, index: Int) {}

    /**
     * Called when Target is ended which is once its exit transition has ended.
     */
    fun onEnded(target: Target, index: Int) {}
}
