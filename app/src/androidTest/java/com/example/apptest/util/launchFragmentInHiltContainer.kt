package com.example.apptest.util

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
import debug.java.com.example.apptest.HiltTestActivity

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
    // Intent explícito que apunta a HiltTestActivity
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).use { scenario ->
        scenario.onActivity { activity ->

            // Si se provee una factory personalizada, la registramos en la Activity
            fragmentFactory?.let {
                activity.supportFragmentManager.fragmentFactory = it
            }

            // Creamos el Fragment usando la factory (o la por defecto)
            val fragment = activity.supportFragmentManager.fragmentFactory
                .instantiate(
                    checkNotNull(T::class.java.classLoader),
                    T::class.java.name
                )

            // Asignamos los argumentos si se proporcionaron (Safe Args bundle)
            fragment.arguments = fragmentArgs

            // Si se proporcionó un NavHostController de test, lo asignamos
            // ANTES de que el Fragment se agregue para que esté disponible en onViewCreated
            navHostController?.let { navController ->
                val observer = object : Observer<LifecycleOwner?> {
                    override fun onChanged(value: LifecycleOwner?) {
                        if (value != null) {
                            Navigation.setViewNavController(fragment.requireView(), navController)
                            fragment.viewLifecycleOwnerLiveData.removeObserver(this) // En caso de que el observer sea llamado más de una vez
                            // para muchos test en el mismo proceso y no acumular observers colgados
                        }
                    }
                }
                fragment.viewLifecycleOwnerLiveData.observeForever(observer)
            }

            // Agregamos el Fragment al contenedor de la Activity
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, fragment, "")
                .commitNow()

            // Ejecutamos el bloque de acción con el Fragment listo
            (fragment as T).action()
        }
    }
}