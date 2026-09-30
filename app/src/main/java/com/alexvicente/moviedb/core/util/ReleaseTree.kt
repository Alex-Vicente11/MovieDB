package com.alexvicente.moviedb.core.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class ReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log(message)
        if(t != null) {
            crashlytics.recordException(t)
        }
    }
}