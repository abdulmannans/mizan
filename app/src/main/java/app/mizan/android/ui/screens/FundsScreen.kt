package app.mizan.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.data.repo.FundRow
import app.mizan.android.ui.FundsViewModel
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.GrowthText
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.ShariahBadge

@Composable
fun FundsScreen(
    onOpenFund: (Long) -> Unit,
    viewModel: FundsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search name, scheme or AMC") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        items(state.rows, key = { it.fund.schemeCode }) { row ->
            FundCard(
                row = row,
                pool = state.settings.availableLumpsum,
                onOpen = { onOpenFund(row.fund.schemeCode) },
                onToggleWatchlist = { viewModel.toggleWatchlist(row) },
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            FootnoteText(Compliance.AMFI_NAMING)
            Spacer(Modifier.height(4.dp))
            FootnoteText("Dip scores compare a fund to its own history. They never rank funds.")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun FundCard(
    row: FundRow,
    pool: Double,
    onOpen: () -> Unit,
    onToggleWatchlist: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable { onOpen() }) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(row.fund.shortName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${row.fund.schemeCode} · ${row.fund.amc}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleWatchlist) {
                    Icon(
                        imageVector = if (row.watchlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (row.watchlisted) "Remove from watchlist" else "Add to watchlist",
                        tint = if (row.watchlisted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Formatters.nav(row.fund.lastNav), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Column {
                    Row {
                        Text(
                            "1D ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GrowthText(row.fund.growth1d)
                        Text(
                            "  1M ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GrowthText(row.fund.growth1m)
                        Text(
                            "  1Y ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GrowthText(row.fund.growth1y)
                    }
                    Text(
                        "NAV as of ${Formatters.date(row.fund.navAsOf)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(row.level, row.score)
                Spacer(Modifier.width(8.dp))
                ShariahBadge(row.fund.shariahStatus)
            }

            if (row.attractiveNow) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Dip opportunity · suggested ${Formatters.money(row.suggestedRupees(pool))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
