package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.alexvicente.moviedb.core.presentation.util.TmdbImage

@Composable
internal fun PosterImage(
    path: String?,
    width: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    AsyncImage(
        model = TmdbImage.poster(path),
        contentDescription = null, // decorativa: el nombre va en el texto contiguo
        contentScale = ContentScale.Crop,
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surface)
    )
}