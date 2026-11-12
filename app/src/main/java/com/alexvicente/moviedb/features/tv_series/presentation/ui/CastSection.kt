package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.presentation.theme.MovieDbTheme
import com.alexvicente.moviedb.core.presentation.util.TmdbImage
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.presentation.CastUiState

@Composable
internal fun CastSection(
    castState: CastUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (castState) {
        is CastUiState.Loading -> CastSectionFrame(modifier) {
            Box(
                modifier = Modifier.fillMaxWidth().height(CAST_ITEM_HEIGHT),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        is CastUiState.Error -> CastSectionFrame(modifier) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.tv_series_cast_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.tv_series_retry))}
            }
        }

        is CastUiState.Success ->
            // Sin reparto no hay sección: un título vacío no aporta nada
            if (castState.members.isNotEmpty()) {
                CastSectionFrame(modifier) { CastRow(castState.members)}
            }
    }
}

@Composable
private fun CastSectionFrame(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.tv_series_details_cast),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun CastRow(members: List<CastMember>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // creditId y no id: la misma persona puede aparecer dos veces con distinto papel
        items(members, key = { it.creditId }) { member -> CastMemberItem(member) }
    }
}

@Composable
private fun CastMemberItem(member: CastMember, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ProfileImage(path = member.profilePath, size = 80.dp)
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (member.character.isNotEmpty()) {
            Text(
                text = member.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileImage(path: String?, size: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = TmdbImage.profile(path),
        contentDescription = null, // decorativa: el nombre va en el texto contiguo
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
    )
}

// Alto aproximado de un elemento, para que la sección no cambie de tamaño al terminar de cargar
private val CAST_ITEM_HEIGHT = 140.dp

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun CastSectionPreview() {
    MovieDbTheme {
        CastSection(
            castState = CastUiState.Success(
                listOf(
                    CastMember(1, "c1", "Ellen Pompeo", "Meredith Grey", null),
                    CastMember(2, "c2", "Chandra Wilson", "Miranda Bailey", null),
                    CastMember(3, "c3", "James Pickens Jr.", "Richard Webber", null),
                    CastMember(4, "c4", "Caterina Scorsone", "Amelia Shepherd", null)
                )
            ),
            onRetry = {}
        )
    }
}