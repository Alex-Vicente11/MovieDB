package com.alexvicente.moviedb.features.tv_series.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme

class TvSeriesFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        setContent {
            MovieDbTheme {
                TvSeriesRoute(
                    onSeriesClick = { seriesId ->
                        val navController = findNavController()
                        if (navController.currentDestination?.id == R.id.tvSeriesFragment) {
                            navController.navigate(
                                TvSeriesFragmentDirections.actionTvSeriesToTvSeriesDetails(seriesId)
                            )
                        }
                    }
                )
            }
        }
    }
}