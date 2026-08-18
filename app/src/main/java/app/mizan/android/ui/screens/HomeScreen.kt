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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.data.db.JOB_STATUS_SUCCESS
import app.mizan.android.data.repo.FundRow
import app.mizan.android.data.repo.HomeState
import app.mizan.android.ui.HomeViewModel
import app.mizan.android.ui.components.DisclaimerChip
import app.mizan.android.ui.components.DisclaimerDialog
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.KeyValueRow
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.SectionCard
import app.mizan.android.ui.components.StatTile

@Composable
fun HomeScreen(
    onOpenFund: (Long) -> Unit,
    onOpenMissed: () -> Unit,
    onOpenFunds: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val home = ui.home
    var showDisclaimer by remember { mutableStateOf(false) }

    if (showDisclaimer) DisclaimerDialog { showDisclaimer = false }

    PullToRefreshBox(
        isRefreshing = ui.refreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val greeting = home.settings.displayName.takeIf { it.isNotBlank() }
                    ?.let { "Assalamu alaikum, $it" } ?: "Mizan"
                Column {
                    Text(greeting, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "SIP first. Extra cash on dips.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { DisclaimerChip(onRead = { showDisclaimer = true }) }

            if (ui.backfillRunning) {
                item {
                    EmptyState(
                        "Loading NAV history in the background. Scores and missed dips fill in as it runs."
                    )
                }
            }

            item { CountsCard(home) }

            item { MissedCard(home, onOpenMissed) }

            item {
                SectionCard(title = "Watchlist today") {
                    if (home.watchlistToday.isEmpty()) {
                        EmptyState("Add funds from Funds.")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onOpenFunds) { Text("Open Funds") }
                    } else {
                        Column {
                            home.watchlistToday.forEachIndexed { index, row ->
                                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                WatchlistTodayRow(row, home, onOpenFund)
                            }
                        }
                    }
                }
            }

            if (home.nextSip != null) {
                item {
                    val sip = home.nextSip
                    SectionCard(title = "Next SIP") {
                        Column {
                            KeyValueRow("Fund", sip.fundName)
                            KeyValueRow("Amount", Formatters.money(sip.amount))
                            KeyValueRow("Debit day", "${sip.debitDay} (${Formatters.date(sip.debitDate)})")
                            KeyValueRow(
                                "Estimated allotment",
                                Formatters.date(sip.allotmentDate),
                            )
                            KeyValueRow(
                                "Allotment NAV vs chart NAV",
                                "${Formatters.nav(sip.estimatedAllotmentNav)} vs ${Formatters.nav(sip.latestNav)}",
                            )
                            Spacer(Modifier.height(8.dp))
                            FootnoteText(Compliance.SIP_ALLOTMENT_HELPER)
                        }
                    }
                }
            }

            item { LastUpdateCard(home) }

            item {
                FootnoteText(Compliance.DISCLAIMER)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CountsCard(home: HomeState) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatTile("Tracked funds", Formatters.count(home.trackedFunds), modifier = Modifier.weight(1f))
            StatTile("Watchlist", Formatters.count(home.watchlistCount), modifier = Modifier.weight(1f))
            StatTile(
                "Signals 7d",
                Formatters.count(home.signalsLast7Days),
                subtitle = "65+",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        StatTile(
            label = "Gold (₹/10g)",
            value = Formatters.money(home.gold?.metal?.lastPrice),
            subtitle = "Silver ₹/kg ${Formatters.money(home.silver?.metal?.lastPrice)}",
        )
        Spacer(Modifier.height(6.dp))
        FootnoteText(Compliance.METAL_QUOTES)
    }
}

@Composable
private fun MissedCard(home: HomeState, onOpenMissed: () -> Unit) {
    val totals = home.missedTotals
    SectionCard(
        title = "Missed deploys",
        trailing = { TextButton(onClick = onOpenMissed) { Text("View") } },
        modifier = Modifier.clickable { onOpenMissed() },
    ) {
        if (totals == null || totals.deploys == 0) {
            EmptyState("No attractive scores (65+) on your watchlist yet.")
        } else {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Deploys", Formatters.count(totals.deploys), modifier = Modifier.weight(1f))
                    StatTile(
                        "Hypothetical P&L",
                        Formatters.moneySigned(totals.suggestedPnl),
                        subtitle = Formatters.percentSigned(totals.suggestedPnlPercent),
                        modifier = Modifier.weight(1.4f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                KeyValueRow(
                    "If ₹10k each → value today",
                    Formatters.money(totals.whatIfValueToday),
                )
                Spacer(Modifier.height(6.dp))
                FootnoteText(Compliance.HYPOTHETICAL)
            }
        }
    }
}

@Composable
private fun WatchlistTodayRow(row: FundRow, home: HomeState, onOpenFund: (Long) -> Unit) {
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
                    "Attractive now · suggested ${Formatters.money(row.suggestedRupees(home.settings.availableLumpsum))}"
                } else {
                    "Not attractive today"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LevelBadge(row.level, row.score)
    }
}

@Composable
private fun LastUpdateCard(home: HomeState) {
    SectionCard(title = "Last update") {
        val job = home.lastJob
        Column {
            KeyValueRow("Finished", Formatters.dateTime(job?.at))
            KeyValueRow("Status", job?.status ?: "never run")
            if (job != null && job.status != JOB_STATUS_SUCCESS) {
                Spacer(Modifier.height(6.dp))
                Text(
                    job.error ?: "The last run did not complete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (home.stale) {
                Spacer(Modifier.height(6.dp))
                FootnoteText("Prices may be old. Pull to refresh.")
            }
        }
    }
}
