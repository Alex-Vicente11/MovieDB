package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme

@Composable
fun TvSeriesScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Series", color = MaterialTheme.colorScheme.onBackground)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun TvSeriesScreenPreview() {
    MovieDbTheme { TvSeriesScreen() }
}