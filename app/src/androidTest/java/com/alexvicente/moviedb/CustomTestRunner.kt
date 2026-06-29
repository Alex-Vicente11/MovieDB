package com.alexvicente.moviedb

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * ¿Por qué se necesita?
 *  MyApplication tiene @HiltAndroidApp, que genera un grafo de dependencias de PRODUCCIÓN.
 *  Los tests instrumentados necesitan su propio grafo, donde @BindValue y @UninstallModules
 *  pueden reemplazar dependencias reales.
 *
 * HiltTestApplication es generada automáticamente por Hilt - es una Application especial
 * diseñada para correr dentro de tests con @HiltAndroidTest
 *
 * newApplication() intercepta la creación de la Application y fuerza el uso de HiltTestApplication
 * en lugar de MyApplication durante los tests
 */

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