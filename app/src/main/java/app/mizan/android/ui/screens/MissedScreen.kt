package app.mizan.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import app.mizan.android.ui.MissedViewModel
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.KeyValueRow
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.SectionCard
import app.mizan.android.ui.components.StatTile

@Composable
fun MissedScreen(
    onOpenFund: (Long) -> Unit,
    viewModel: MissedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val totals = state.totals

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Missed opportunities", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Attractive days (65+) on your watchlist, clustered so one crash counts once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "Suggested-₹ scenario") {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTile("Deploys", Formatters.count(totals.deploys), modifier = Modifier.weight(1f))
                        StatTile(
                            "Would-have-invested",
                            Formatters.money(totals.suggestedInvested),
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTile(
                            "Value today",
                            Formatters.money(totals.suggestedValueToday),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            "P&L",
                            Formatters.moneySigned(totals.suggestedPnl),
                            subtitle = Formatters.percentSigned(totals.suggestedPnlPercent),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "If ${Formatters.money(state.settings.whatIfAmount)} each") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile(
                        "Invested",
                        Formatters.money(totals.whatIfInvested),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        "Value today",
                        Formatters.money(totals.whatIfValueToday),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        "P&L",
                        Formatters.moneySigned(totals.whatIfPnl),
                        subtitle = Formatters.percentSigned(totals.whatIfPnlPercent),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            SectionCard(title = "Today") {
                if (state.watchlistEmpty) {
                    EmptyState("Add funds from the Funds list.")
                } else {
                    Column {
                        state.today.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenFund(row.fund.schemeCode) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.fund.shortName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (row.attractiveNow) {
                                            "Attractive now · suggested " +
                                                Formatters.money(
                                                    row.suggestedRupees(state.settings.availableLumpsum)
                                                )
                                        } else {
                                            "Not attractive now"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                LevelBadge(row.level, row.score)
                            }
                        }
                    }
                }
            }
        }

        item { Text("Missed dips", style = MaterialTheme.typography.titleMedium) }

        if (state.deploys.isEmpty()) {
            item {
                EmptyState(
                    if (state.watchlistEmpty) {
                        "Add funds from the Funds list."
                    } else {
                        "No attractive scores (65+) on your watchlist yet."
                    }
                )
            }
        } else {
            items(state.deploys, key = { "${it.targetId}-${it.date}" }) { deploy ->
                SectionCard(
                    title = deploy.name,
                    trailing = { LevelBadge(deploy.level, deploy.score) },
                    modifier = Modifier.clickable {
                        deploy.targetId.toLongOrNull()?.let(onOpenFund)
                    },
                ) {
                    Column {
                        KeyValueRow("Best score in cluster", Formatters.date(deploy.date))
                        KeyValueRow(
                            "NAV then → now",
                            "${Formatters.nav(deploy.navThen)} → ${Formatters.nav(deploy.navNow)}",
                        )
                        KeyValueRow("Suggested ₹", Formatters.money(deploy.suggestedRupees))
                        KeyValueRow("Value today", Formatters.money(deploy.valueToday))
                        KeyValueRow(
                            "If ${Formatters.money(deploy.whatIfInvested)}",
                            "${Formatters.money(deploy.whatIfValueToday)} (${Formatters.moneySigned(deploy.whatIfPnl)})",
                        )
                        KeyValueRow("Return", Formatters.percentSigned(deploy.returnPercent))
                    }
                }
            }
        }

        item {
            FootnoteText(Compliance.HYPOTHETICAL)
            Spacer(Modifier.height(6.dp))
            FootnoteText(Compliance.DISCLAIMER)
            Spacer(Modifier.height(6.dp))
            FootnoteText("Metals are not on this page: they are not watchlisted.")
            Spacer(Modifier.height(24.dp))
        }
    }
}
