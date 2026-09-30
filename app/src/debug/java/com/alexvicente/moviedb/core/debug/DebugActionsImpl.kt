package com.alexvicente.moviedb.core.debug

import javax.inject.Inject

class DebugActionsImpl @Inject constructor() : DebugActions {
    override fun triggerTestCrash() {
        throw RuntimeException("Test Crash") // Crash intencional para verificar Crashlytics
    }
}