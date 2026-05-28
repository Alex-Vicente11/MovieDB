package com.example.apptest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.example.apptest.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 *  CAMBIOS vs versión anterior:
 *  AGREGADO -> binding.bottomNavigationView.setupWithNavController(navController)
 *  CAMBIADO -> AppBarConfiguration ahora incluye favoritesFragment como top-level
 *
 *  setupWithNavController() en el BottomNavigationView hace automáticamente:
 *      Al tocar una pestaña -> navega al fragment correspondiente
 *      Gestiona el back stack para cada pestaña independiente
 *      Restaura el estado de cada pestaña al volver a ella
 *
 * AppBarConfiguration con 2 top-level destinations:
 *    popularMoviesFragment y favoritesFragment son top-level -> sin botón <-
 *    movieDetailsFragment y videosFragment son secundarios -> con botón <-
 */

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Ambas pestañas del BottomNav son top-level -> sin botón <-
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.popularMoviesFragment,
                R.id.favoritesFragment
            )
        )
        /**
         * Conectar BottomNavigationView con NavController
         * Cada item del menú tiene el mismo id con su destination en el grafo
         * Navigation Component maneja la navegación automáticamente
         */
        binding.bottomNavigationView.setupWithNavController(navController)
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