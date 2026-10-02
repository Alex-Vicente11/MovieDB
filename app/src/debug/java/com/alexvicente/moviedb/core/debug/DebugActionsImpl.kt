package com.alexvicente.moviedb.core.debug

import javax.inject.Inject

class DebugActionsImpl @Inject constructor() : DebugActions {
    override fun triggerTestCrash() {
        throw RuntimeException("Test Crash") // Crash intencional para verificar Crashlytics
    }

    override fun triggerTestAnr() {
        Thread.sleep(10_000) // Bloquea el hilo principal 10s - dispara un ANR real
    }
}