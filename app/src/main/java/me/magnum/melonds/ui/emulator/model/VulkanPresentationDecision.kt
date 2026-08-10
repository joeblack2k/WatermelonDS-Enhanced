package me.magnum.melonds.ui.emulator.model

import me.magnum.enhancements.EnhancementPresentationState
import me.magnum.enhancements.requiresNative43Fallback

internal fun useNative43Fallback(state: EnhancementPresentationState?): Boolean =
    state?.requiresNative43Fallback() == true
