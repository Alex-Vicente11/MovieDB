package com.alexvicente.moviedb

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class CustomTestRunner: AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        // Ignora el className original (MyApplication) y siempre usa HiltTestApplication
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}