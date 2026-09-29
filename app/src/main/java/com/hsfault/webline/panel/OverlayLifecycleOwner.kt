package com.hsfault.webline.panel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/** Compose needs a lifecycle to run inside a service window (there's no Activity here). */
class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = registry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedState.savedStateRegistry

    fun start() {
        if (registry.currentState != Lifecycle.State.INITIALIZED) return
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}