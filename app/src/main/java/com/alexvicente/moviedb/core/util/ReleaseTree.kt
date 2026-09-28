package com.alexvicente.moviedb.core.util

import android.util.Log
import timber.log.Timber

class ReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return

        // Punto de extensión futuro: aquí se reportaría a Crashlytics
        // cuando lo agregues al roadmap (ej. FirebaseCrashlytics.getInstance()
        //     .log(message) y .recordException(t) si t != null)
    }
}