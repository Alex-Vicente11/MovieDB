package com.example.apptest

import android.app.Application
import com.example.apptest.di.AppContainer

/**
 * CLASE APPLICATION
 *
 * UBICACIÓN: raíz del package base (com.example.apptest/)
 *
 * Responsabilidad:
 * - Mantener el AppContainer durante toda la vida de la app
 * - Inicializar librerías globales si es necesario
 * - Se crea ANTES que cualquier Activity
 *
 * Ciclo de vida:
 * 1. onCreate() se llama cuando la app inicia
 * 2. La app corre (Activities, Services, etc.)
 * 3. onTerminate() se llama cuando la app se cierra (solo en emulador)
 *
 * IMPORTANTE: Debe estar registrada en AndroidManifest.xml:
 * ```xml
 * <application
 *     android:name=".MyApplication"
 *     ...>
 * </application>
 * ```
 *
 * Cambios vs versión legacy:
 *  Package actualizado: movies → raíz (com.example.apptest)
 * Import actualizado: AppContainer ahora está en di/
 * Documentación mejorada
 */
class MyApplication : Application() {

    /**
     * Contenedor de dependencias (Service Locator)
     *
     * Inicialización lazy:
     * - Se crea solo cuando se accede por primera vez
     * - Singleton durante toda la vida de la app
     *
     * Acceso desde Activities:
     * ```kotlin
     * val appContainer = (application as MyApplication).appContainer
     * val useCase = appContainer.searchContainer.searchMoviesUseCase
     * ```
     */
    val appContainer: AppContainer by lazy {
        AppContainer()
    }

    /**
     * Se llama cuando la app se inicia (antes de cualquier Activity)
     *
     * Aquí puedes inicializar librerías globales:
     * - Timber: Logging mejorado
     * - LeakCanary: Detección de memory leaks
     * - Firebase: Analytics, Crashlytics
     * - WorkManager: Tareas en background
     */
    override fun onCreate() {
        super.onCreate()

        // Ejemplo de inicialización de librerías:
        // if (BuildConfig.DEBUG) {
        //     Timber.plant(Timber.DebugTree())
        // }
    }
}