package app.mizan.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Formatters
import app.mizan.android.data.repo.FundRow
import app.mizan.android.data.repo.HomeState
import app.mizan.android.ui.HomeViewModel
import app.mizan.android.ui.components.ComplianceChip
import app.mizan.android.ui.components.DataRow
import app.mizan.android.ui.components.DisclaimerDialog
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.EntityRow
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.MizanCard
import app.mizan.android.ui.components.SectionHeader
import app.mizan.android.ui.components.Stat
import app.mizan.android.ui.components.StatStrip
import app.mizan.android.ui.theme.Shapes
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

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
            contentPadding = PaddingValues(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            item {
                val greeting = home.settings.displayName.takeIf { it.isNotBlank() }
                    ?.let { "Assalamu alaikum, $it" } ?: "Mizan"
                Column {
                    Text(greeting, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Market dip alerts for smart investing.",
                        style = TextRole.secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { ComplianceChip(onRead = { showDisclaimer = true }) }

            if (ui.backfillRunning) {
                item {
                    Banner("Loading NAV history. Scores and missed dips fill in as it runs.")
                }
            }

            item { RefreshStatusBanner(home) }

            item { TodayHeadline(home) }

            item {
                SectionHeader(
                    "Watchlist today",
                    trailing = {
                        if (home.watchlistToday.isNotEmpty()) {
                            TextButton(onClick = onOpenFunds) { Text("Add") }
                        }
                    },
                )
            }

            item {
                if (home.watchlistToday.isEmpty()) {
                    Column {
                        EmptyState("Add funds from Funds to see them here.")
                        Spacer(Modifier.height(Space.sm))
                        TextButton(onClick = onOpenFunds) { Text("Open Funds") }
                    }
                } else {
                    MizanCard {
                        home.watchlistToday.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider()
                            WatchlistTodayRow(row, home, onOpenFund)
                        }
                    }
                }
            }

            item { MissedSummary(home, onOpenMissed) }

            item { Snapshot(home) }

            item { LastUpdate(home) }

            item { Spacer(Modifier.height(Space.xl)) }
        }
    }
}

@Composable
private fun RefreshStatusBanner(home: HomeState) {
    val job = home.lastJob
    val attractive = home.watchlistToday.count { it.attractiveNow }
    val summary = when {
        job == null -> "No data yet. Pull to refresh or wait for the first update."
        !job.succeeded -> "Last update failed. Pull to refresh."
        attractive > 0 -> "$attractive fund${if (attractive > 1) "s" else ""} attractive. Updated ${Formatters.dateTime(job.at)}"
        else -> "All calm. Updated ${Formatters.dateTime(job.at)}"
    }
    val isError = job != null && !job.succeeded
    Banner(text = summary, error = isError)
}

/** The one question Home exists to answer. */
@Composable
private fun TodayHeadline(home: HomeState) {
    val attractive = home.watchlistToday.filter { it.attractiveNow }
    val suggested = attractive.sumOf { it.suggestedRupees(home.settings.availableLumpsum) ?: 0.0 }
    MizanCard {
        Text(
            when {
                home.watchlistToday.isEmpty() -> "Nothing tracked yet"
                attractive.isEmpty() -> "Nothing to act on today"
                attractive.size == 1 -> "1 fund is attractive today"
                else -> "${attractive.size} funds are attractive today"
            },
            style = TextRole.figure,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            when {
                home.watchlistToday.isEmpty() ->
                    "Star a fund in Funds and Mizan starts scoring it against its own history."
                attractive.isEmpty() -> "Scores are below 65. No action needed today."
                else -> "Suggested extra lumpsum ${Formatters.money(suggested)} from your pool."
            },
            style = TextRole.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MissedSummary(home: HomeState, onOpenMissed: () -> Unit) {
    val totals = home.missedTotals
    MizanCard {
        EntityRow(
            title = "Missed deploys",
            subtitle = if (totals == null || totals.deploys == 0) {
                "No attractive scores (65+) on your watchlist yet"
            } else {
                "${Formatters.count(totals.deploys)} clustered · " +
                    "${Formatters.moneySigned(totals.suggestedPnl)} hypothetical · " +
                    "₹10k each would be ${Formatters.money(totals.whatIfValueToday)}"
            },
            onClick = onOpenMissed,
            trailing = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
        )
    }
}

@Composable
private fun Snapshot(home: HomeState) {
    MizanCard {
        StatStrip(
            listOf(
                Stat("Tracked", Formatters.count(home.trackedFunds)),
                Stat("Watchlist", Formatters.count(home.watchlistCount)),
                Stat("Signals 7d", Formatters.count(home.signalsLast7Days), "65+"),
            )
        )
        Spacer(Modifier.height(Space.sm))
        HorizontalDivider()
        DataRow("Gold ₹/10g", Formatters.money(home.gold?.metal?.lastPrice))
        DataRow("Silver ₹/kg", Formatters.money(home.silver?.metal?.lastPrice))
    }
}

@Composable
private fun WatchlistTodayRow(row: FundRow, home: HomeState, onOpenFund: (Long) -> Unit) {
    EntityRow(
        title = row.fund.shortName,
        subtitle = if (row.attractiveNow) {
            "Attractive · suggested " +
                Formatters.money(row.suggestedRupees(home.settings.availableLumpsum))
        } else {
            "Not attractive today"
        },
        onClick = { onOpenFund(row.fund.schemeCode) },
        trailing = { LevelBadge(row.level, row.score) },
    )
}

/** One line when the pipeline is healthy, a banner only when it is not. */
@Composable
private fun LastUpdate(home: HomeState) {
    val job = home.lastJob
    val failed = job != null && !job.succeeded
    if (failed || home.stale) {
        Banner(
            text = when {
                failed -> job.error ?: "The last update did not complete. Pull to refresh."
                else -> "Prices may be old. Pull to refresh."
            },
            error = failed,
        )
    } else {
        Text(
            "Updated ${Formatters.dateTime(job?.at)}",
            style = TextRole.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.xs),
        )
    }
}

@Composable
private fun Banner(text: String, error: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.field,
        color = if (error) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text,
            style = TextRole.secondary,
            color = if (error) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(Space.md),
        )
    }
}
