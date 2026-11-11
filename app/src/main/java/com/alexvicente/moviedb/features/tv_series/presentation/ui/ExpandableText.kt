package com.alexvicente.moviedb.features.tv_series.presentation.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.alexvicente.moviedb.R

@Composable
internal fun ExpandableText(
    text: String,
    collapsedMaxLines: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var canExpand by remember { mutableStateOf(false) }
    val toggleable = canExpand || expanded

    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) canExpand = result.hasVisualOverflow },
            modifier = Modifier.clickable(enabled = toggleable) { expanded = !expanded }
        )
        if (toggleable) {
            Text(
                text = stringResource(
                    if (expanded) R.string.tv_series_episode_see_less
                    else R.string.tv_series_episode_see_more
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}