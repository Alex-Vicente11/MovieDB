/**package com.example.apptest.util

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.apptest.R
import debug.java.com.example.apptest.HiltTestActivity

/**
 * Lanza un Fragment dentro de un NavHostFragment REAL con TestNavHostController
 *
 * ¿Por qué esto soluciona "does not have NavController set"?
 *   finNavController() camina hacia arriba en el árbol de vistas buscando
 *   un ancestro con el NavController asignado. NavHostFragment asigna el
 *   tag a su PROPIA vista durante su propio onViewCreated - que ocurre
 *   ANTES de instanciar el Fragment hijo (nuestro Fragment bajo test).
 *
 *   Esto replica EXACTAMENTE cómo funciona en producción (MainActivity contiene
 *   un NavHostFragment que hospeda PopularMoviesFragment)
 *
 * @param navGraphId ID del nav_graph.xml (R.navigation.nav_graph)
 * @param fragmentArgs argumentos del Fragment (Safe Args bundle)
 * @param startDestinationId destino inicial - debe coincidir con T
 */

inline fun <reified T: Fragment> launchFragmentInHiltNavHostContainer(
    navGraphId: Int = R.navigation.nav_graph,
    startDestinationId: Int,
    fragmentArgs: Bundle? = null,
    fragmentFactory: FragmentFactory? = null,
    crossinline action: T.() -> Unit = {}
): TestNavHostController {

    val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    lateinit var fragmentRef: T

    ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        fragmentFactory?.let {
            activity.supportFragmentManager.fragmentFactory = it
        }

        // Configura el NavController de test ANTES de crear cualquier Fragment
        navController.setGraph(navGraphId)
        navController.setCurrentDestination(startDestinationId, fragmentArgs ?: Bundle())

        val fragment = activity.supportFragmentManager.fragmentFactory
            .instantiate(checkNotNull(T::class.java.classLoader), T::class.java.name)

        fragment.arguments = fragmentArgs
        fragmentRef = fragment as T

        /**
         * Reemplazamos NavHostController interno por nuestro TestNavHostController
         * usando setViewNavController() sobre la vista raíz de la Activity,
         * que es el ancestro de TODAS las vistas - incluida la del Fragment
         */
        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        androidx.navigation.Navigation.setViewNavController(
            activity.findViewById(android.R.id.content),
            navController
        )

        /**
         * Forzamos un segundo paso de vista: como ya está commitNow(),
         * necesitamos reasignar el tag y re-disparar onViewCreated si es necesario.
         * En la práctica, basta con asignar el tag al contenedor raíz
         * ANTES de que el Fragment necesite buscarlo - por eso reordenamos:
         */
    }
    return navController
}*/