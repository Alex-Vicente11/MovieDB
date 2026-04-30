package com.example.apptest

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * CAMBIO vs versión anterior:
 *
 * ANTES:
 *   class MyApplication : Application() {
 *       val appContainer: AppContainer by lazy { AppContainer() }
 *   }
 *
 * DESPUÉS:
 *   @HiltAndroidApp
 *   class MyApplication : Application()
 *
 * ────────────────────────────────────────────────────────────────────────────
 * ¿Qué hace @HiltAndroidApp?
 * ────────────────────────────────────────────────────────────────────────────
 * Esta anotación le ordena a kapt (el procesador de anotaciones) que genere
 * el componente raíz de Hilt: SingletonComponent.
 *
 * En tiempo de compilación, kapt genera una clase llamada:
 *   MyApplication_GeneratedInjector.java  (nunca la ves tú)
 *
 * Esa clase contiene el grafo de dependencias completo de la app:
 *   - OkHttpClient (Singleton)
 *   - Retrofit (Singleton)
 *   - Todos los Repository (Singleton)
 *   - Todos los UseCase (creados por @Inject constructor)
 *   - Todos los ViewModel (ViewModelScoped, via @HiltViewModel)
 *
 * SOLID → Single Responsibility:
 *   MyApplication ya no tiene ninguna responsabilidad de creación de objetos.
 *   Hilt asume toda esa responsabilidad en tiempo de compilación.
 *
 * BENEFICIO:
 *   Si olvidaste proveer una dependencia en algún módulo, el error aparece
 *   al compilar — no como un NullPointerException en runtime.
 */
@HiltAndroidApp
class MyApplication: Application()