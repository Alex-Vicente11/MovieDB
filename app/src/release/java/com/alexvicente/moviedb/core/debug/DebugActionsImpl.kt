package com.alexvicente.moviedb.core.debug

import javax.inject.Inject

class DebugActionsImpl @Inject constructor() : DebugActions {
    override fun triggerTestCrash() {
        // No-op intencional: en release este botón no debe hacer nada.
    }

    override fun triggerTestAnr() {
        // No-op intencional
    }
}