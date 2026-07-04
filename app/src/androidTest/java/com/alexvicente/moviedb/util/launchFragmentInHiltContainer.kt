package com.alexvicente.moviedb.util

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import debug.java.com.alexvicente.apptest.HiltTestActivity

/**
 * Reemplaza launchFragmentInContainer para proyectos con Hilt
 *
 * ¿Qué hace?
 *   1. Lanza HiltTestActivity (que tiene @AndroidEntryPoint)
 *   2. Agrega el Fragment al contenedor de esa Activity
 *   3. Ejecuta el bloque de acción con el Fragment listo
 *
 * ¿Por qué es una función inline con reified T?
 *   reified permite acceder al tipo T en runtime sin reflexión manual.
 *   Esto nos deja hacer Fragment() genérico con el tipo correcto.
 *
 * @param fragmentArgs Bundle de argumentos para el Fragment (Safe Args)
 * @param navHostController TestNavHostController para verificar navegación
 * @param fragmentFactory Factory personalizada si el Fragment necesita parámetros
 * @param action Bloque ejecutado con el Fragment ya lanzado y visible
 */

inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    navHostController: TestNavHostController? = null,
    fragmentFactory: FragmentFactory? = null,
    crossinline action: T.() -> Unit = {}
) {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    // ❌ ANTES: .use { scenario -> ... } cerraba la Activity
    //    automáticamente al salir del bloque, ANTES de que
    //    Espresso pudiera hacer onView(...).check(...)
    //
    // ✅ AHORA: sin .use{} — la Activity permanece RESUMED
    //    durante todoo el test. Se destruye al finalizar el
    //    proceso de test (o se podría cerrar manualmente
    //    en @After si se requiere limpieza explícita)
    val scenario = ActivityScenario.launch<HiltTestActivity>(startActivityIntent)

    scenario.onActivity { activity ->

        fragmentFactory?.let {
            activity.supportFragmentManager.fragmentFactory = it
        }

        val fragment = activity.supportFragmentManager.fragmentFactory
            .instantiate(
                checkNotNull(T::class.java.classLoader),
                T::class.java.name
            )
        fragment.arguments = fragmentArgs

        // CLAVE: el contenedor (android.R.id.content) YA EXISTE
        // desde que la Activity se crea — antes de agregar el Fragment.
        // findNavController() camina hacia arriba desde la vista del
        // Fragment y encontrará el tag en este contenedor PADRE,
        // sin depender de timing de onCreateView/onViewCreated del Fragment.
        // Observer con lateinit var — evita memory leak y auto-se-remueve
        navHostController?.let { navController ->
            lateinit var observer: Observer<LifecycleOwner?>
            observer = Observer { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    Navigation.setViewNavController(fragment.requireView(), navController)
                    fragment.viewLifecycleOwnerLiveData.removeObserver(observer)
                }
            }
            fragment.viewLifecycleOwnerLiveData.observeForever(observer)
        }

        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        (fragment as T).action()
    }
    // scenario permanece abierto — Espresso lo cierra automáticamente
    // al final del test cuando el proceso termina naturalmente
}
