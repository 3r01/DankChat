package com.flxrs.dankchat.ui.main.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.flxrs.dankchat.ui.chat.suggestion.Suggestion
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SuggestionDropdown(
    suggestions: ImmutableList<Suggestion>,
    onSuggestionClick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        modifier = modifier,
        enter =
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 4 },
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            ) +
                fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                ) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ),
        exit =
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 4 },
            ) + fadeOut() + scaleOut(targetScale = 0.92f),
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(suggestions) {
            listState.scrollToItem(0)
        }

        OutlinedCard(
            modifier =
                Modifier
                    .padding(horizontal = 2.dp)
                    .fillMaxWidth(0.66f)
                    .heightIn(max = 250.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
            ) {
                items(suggestions, key = { it.toString() }) { suggestion ->
                    SuggestionItem(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: Suggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon/Image based on suggestion type
        when (suggestion) {
            is Suggestion.EmoteSuggestion -> {
                AsyncImage(
                    model = suggestion.emote.url,
                    contentDescription = suggestion.emote.code,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(end = 12.dp),
                )
                Text(
                    text = suggestion.emote.code,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is Suggestion.UserSuggestion -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .padding(end = 12.dp),
                )
                Text(
                    text = suggestion.name.value,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is Suggestion.EmojiSuggestion -> {
                Text(
                    text = suggestion.emoji.unicode,
                    fontSize = 24.sp,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .padding(end = 12.dp)
                            .wrapContentSize(),
                )
                Text(
                    text = ":${suggestion.emoji.code}:",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is Suggestion.CommandSuggestion -> {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .padding(end = 12.dp),
                )
                Text(
                    text = suggestion.command,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is Suggestion.FilterSuggestion -> {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = suggestion.displayText ?: suggestion.keyword,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(suggestion.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
