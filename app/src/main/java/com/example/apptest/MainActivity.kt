package com.example.apptest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.apptest.databinding.ActivityMainBinding

/**
 * Single Activity Container
 *
 * Single Responsibility Principle:
 * Antes: MainActivity manejaba UI, búsqueda, adapter, ViewModel, navegación
 * Ahora: solo gestiona el NavController y el botón Atrás del sistema.
 * El resto se delegó a PopularMoviesFragment
 *
 * Separation of Concerns:
 *  Activity = infraestructura Android (ciclo de vida, window, system back)
 *  Fragment = unidad de UI + lógica de presentación
 *  ViewModel = estado de UI + llamadas a UseCases
 *  UseCase/Repository = lógica de negocio y datos
 *
 *  Ventajas: vs múltiples Activies:
 *      Transiciones y animaciones fluidas entre pantallas
 *      ViewModels compartidos entre Fragments (mismo Scope de Activity)
 *      Back Stack gestionando automáticamente por el Navigation Component
 *      Un solo punto de entrada -> fácil de depurar y probar
 *      Preparado para Deep Links (un intent = una destionation en el grafo)
 */

class MainActivity: AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavController()
    }

    /**
     * Obtiene el NavController desde el NavHostFragment
     *
     * IMPORTANTE: Usar finNavController(R.id.nav_host_fragment) directamente desde la Activity
     * puede fallar si el Fragment aún no está adjunto.
     * La forma correcta es obtenerlo a través del NavHostFragment directamente
     *
     * AppBarConfiguration le dice al NavController cuáles son las pantallas
     * "raíz" (top-level destinations). En estas pantallas:
     *      .El botón <- (Up) NO aparece en la Toolbar
     *      .El botón Back del sistema cierra la app (no navega atrás)
     *
     */
    private fun setupNavController() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.popularMoviesFragment)
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()

        _binding = null
    }

}