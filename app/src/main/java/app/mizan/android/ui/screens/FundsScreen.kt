package app.mizan.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Formatters
import app.mizan.android.data.repo.FundRow
import app.mizan.android.ui.FundsViewModel
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.ShariahBadge
import app.mizan.android.ui.theme.Shapes
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

@Composable
fun FundsScreen(
    onOpenFund: (Long) -> Unit,
    viewModel: FundsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search name, scheme or AMC") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = Shapes.field,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.lg, vertical = Space.sm),
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Space.xl),
        ) {
            if (state.rows.isEmpty()) {
                item {
                    EmptyState(
                        "No funds match that search.",
                        Modifier.padding(Space.lg),
                    )
                }
            }

            items(state.rows, key = { it.fund.schemeCode }) { row ->
                FundListRow(
                    row = row,
                    pool = state.settings.availableLumpsum,
                    onOpen = { onOpenFund(row.fund.schemeCode) },
                    onToggleWatchlist = { viewModel.toggleWatchlist(row) },
                )
                HorizontalDivider()
            }

            item {
                Spacer(Modifier.height(Space.md))
                FootnoteText(
                    "Dip scores compare a fund to its own history. They never rank funds.",
                    Modifier.padding(horizontal = Space.lg),
                )
            }
        }
    }
}

@Composable
private fun FundListRow(
    row: FundRow,
    pool: Double,
    onOpen: () -> Unit,
    onToggleWatchlist: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onOpen),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        overlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShariahBadge(row.fund.shariahStatus)
                Spacer(Modifier.width(Space.sm))
                Text(
                    row.fund.amc,
                    style = TextRole.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        headlineContent = {
            Text(
                row.fund.shortName,
                style = TextRole.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                Text(Formatters.nav(row.fund.lastNav), style = TextRole.figureSmall)
                Text(
                    "1D ${Formatters.percentSigned(row.fund.growth1d)}   " +
                        "1M ${Formatters.percentSigned(row.fund.growth1m)}   " +
                        "1Y ${Formatters.percentSigned(row.fund.growth1y)}",
                    style = TextRole.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            IconButton(onClick = onToggleWatchlist) {
                Icon(
                    imageVector = if (row.watchlisted) {
                        Icons.Filled.Star
                    } else {
                        Icons.Outlined.StarOutline
                    },
                    contentDescription = if (row.watchlisted) {
                        "Remove from watchlist"
                    } else {
                        "Add to watchlist"
                    },
                    tint = if (row.watchlisted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                LevelBadge(row.level, row.score)
                if (row.attractiveNow) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "extra ${Formatters.money(row.suggestedRupees(pool))}",
                        style = TextRole.caption,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}
