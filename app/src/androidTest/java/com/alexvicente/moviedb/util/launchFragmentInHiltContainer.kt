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
 * Lanza un Fragment @AndroidEntryPoint dentro de una Activity de test con Hilt.
 *
 * Devuelve el ActivityScenario para que el test pueda acceder a la Activity y a su
 * FragmentManager (por ejemplo, para recrear la vista con detach/attach).
 */
inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    navHostController: TestNavHostController? = null,
    fragmentFactory: FragmentFactory? = null,
    crossinline action: T.() -> Unit = {}
): ActivityScenario<HiltTestActivity> {   // CAMBIO 1: antes devolvía Unit

    // 1. Intent que arranca la Activity de test (declarada en el manifest de debug)
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    // 2. Lanza la Activity
    val scenario = ActivityScenario.launch<HiltTestActivity>(startActivityIntent)

    // 3. Con la Activity ya creada (hilo principal), monta el Fragment
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

        // 4. Asocia el NavController de test a la vista del Fragment
        navHostController?.let { navController ->
            // CAMBIO 2: el observer ya NO se quita tras la primera vista. Cada vez que se
            // crea una vista nueva (detach/attach, vuelta del back stack) hay que volver a
            // asociarle el NavController, o findNavController() fallaría en la segunda vista.
            val observer = Observer<LifecycleOwner?> { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    Navigation.setViewNavController(fragment.requireView(), navController)
                }
            }
            fragment.viewLifecycleOwnerLiveData.observeForever(observer)
        }

        // 5. Añade el Fragment a la Activity y ejecuta la transacción de inmediato
        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        // 6. Permite al test ejecutar código sobre el Fragment ya creado
        (fragment as T).action()
    }

    return scenario   // CAMBIO 3: devuelve el scenario al test
}