package com.example.apptest

import android.app.Application
import com.example.apptest.di.AppContainer

/**
 * CLASE APPLICATION
 *
 * Se crea cuando la app inicia (antes que cualquier Activity)
 * Mantiene el contenedor de dependencias durante toda la vida de la app
 *
 * IMPORTANTE: Registrar en AndroidManifest.xml:
 * <application android:name=".MyApplication" ...>
 */
class MyApplication : Application() {

    /**
     * Contenedor de dependencias
     * Accesible desde todas las Activities/Fragments
     */
    val appContainer: AppContainer by lazy {
        AppContainer()
    }

    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar otras librerías:
        // - Timber para logs
        // - LeakCanary para memory leaks
        // - Firebase Analytics
    }
}