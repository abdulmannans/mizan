package app.mizan.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.data.db.FundEntity
import app.mizan.android.data.repo.FundDetailState
import app.mizan.android.domain.AllocationBands
import app.mizan.android.domain.PricePoint
import app.mizan.android.ui.FundDetailViewModel
import app.mizan.android.ui.components.ComplianceChip
import app.mizan.android.ui.components.DataRow
import app.mizan.android.ui.components.DetailPane
import app.mizan.android.ui.components.DetailPaneSelector
import app.mizan.android.ui.components.DisclaimerDialog
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.EntityRow
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.GrowthText
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.LineChart
import app.mizan.android.ui.components.MizanCard
import app.mizan.android.ui.components.ScoreBreakdown
import app.mizan.android.ui.components.ShariahBadge
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

private const val SIGNAL_PAGE_SIZE = 60

@Composable
fun FundDetailScreen(
    onBack: () -> Unit,
    viewModel: FundDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fund = state.fund
    var showDisclaimer by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(DetailPane.OVERVIEW) }

    if (showDisclaimer) DisclaimerDialog { showDisclaimer = false }

    if (showDetails && fund != null) {
        FundDetailsSheet(fund = fund, onDismiss = { showDetails = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fund?.shortName ?: "Fund") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDetails = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Fund details")
                    }
                    IconButton(onClick = { viewModel.toggleWatchlist(state.watchlisted) }) {
                        Icon(
                            imageVector = if (state.watchlisted) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarOutline
                            },
                            contentDescription = if (state.watchlisted) {
                                "Remove from watchlist"
                            } else {
                                "Add to watchlist"
                            },
                            tint = if (state.watchlisted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (fund == null) {
            EmptyState("Fund not found.", Modifier.padding(padding).padding(Space.lg))
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            item { PriceHeader(fund, state) }

            item { LineChart(points = state.navSeries) }

            item { DetailPaneSelector(selected = tab, onSelect = { tab = it }) }

            when (tab) {
                DetailPane.OVERVIEW -> overviewTab(state)
                DetailPane.SCORE -> scoreTab(state)
                DetailPane.HISTORY -> historyTab(state)
            }

            item {
                ComplianceChip(onRead = { showDisclaimer = true })
                Spacer(Modifier.height(Space.xl))
            }
        }
    }
}

@Composable
private fun PriceHeader(fund: FundEntity, state: FundDetailState) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Formatters.nav(fund.lastNav), style = TextRole.figure)
            Spacer(Modifier.width(Space.md))
            LevelBadge(state.latest?.level, state.latest?.score)
        }
        Spacer(Modifier.height(Space.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("1D ", style = TextRole.caption)
            GrowthText(fund.growth1d)
            Text("   1M ", style = TextRole.caption)
            GrowthText(fund.growth1m)
            Text("   1Y ", style = TextRole.caption)
            GrowthText(fund.growth1y)
        }
        Text(
            "NAV as of ${Formatters.date(fund.navAsOf)}",
            style = TextRole.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.overviewTab(state: FundDetailState) {
    item {
        val latest = state.latest
        MizanCard(title = "Dip opportunity") {
            if (latest == null) {
                EmptyState("Not scored yet.")
            } else {
                Text(
                    "${AllocationBands.suggestedPercent(latest.score)}% of your pool · " +
                        Formatters.money(
                            AllocationBands.suggestedRupees(
                                latest.score,
                                state.settings.availableLumpsum,
                            )
                        ),
                    style = TextRole.figureSmall,
                )
                Text(
                    "Scored ${Formatters.date(latest.date)} at NAV ${Formatters.nav(latest.value)}",
                    style = TextRole.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.md))
                latest.reasons.forEach { reason ->
                    Text("• $reason", style = TextRole.secondary)
                }
                Spacer(Modifier.height(Space.md))
                FootnoteText(Compliance.OVERLAY_NEEDS_CASH)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.scoreTab(state: FundDetailState) {
    item {
        val latest = state.latest
        MizanCard(title = "Components") {
            if (latest == null) {
                EmptyState("Not scored yet.")
            } else {
                ScoreBreakdown(latest.components)
            }
        }
    }

    item {
        MizanCard(title = "Score over time") {
            LineChart(
                points = state.signals
                    .sortedBy { it.date }
                    .map { PricePoint(it.date, it.score.toDouble()) },
                valueFormatter = { "%.0f".format(it) },
                fixedRange = 0f..100f,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.historyTab(state: FundDetailState) {
    if (state.signals.isEmpty()) {
        item { EmptyState("No scores yet. History fills in after the first update.") }
        return
    }

    items(state.signals.take(SIGNAL_PAGE_SIZE), key = { it.date.toEpochDay() }) { signal ->
        EntityRow(
            title = Formatters.date(signal.date),
            subtitle = "NAV ${Formatters.nav(signal.value)}" +
                (signal.reasons.firstOrNull()?.let { " · $it" } ?: ""),
            trailing = { LevelBadge(signal.level, signal.score) },
        )
        HorizontalDivider()
    }

    item {
        if (state.signals.size > SIGNAL_PAGE_SIZE) {
            FootnoteText(
                "Showing the latest $SIGNAL_PAGE_SIZE of ${state.signals.size} scores."
            )
        }
    }
}

@Composable
private fun FundDetailsSheet(fund: FundEntity, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.lg)
                .padding(bottom = Space.xl),
        ) {
            Text(fund.name, style = TextRole.title)
            Spacer(Modifier.height(Space.sm))
            ShariahBadge(fund.shariahStatus)
            Spacer(Modifier.height(Space.md))
            DataRow("Scheme code", fund.schemeCode.toString())
            DataRow("AMC", fund.amc)
            DataRow("ISIN", fund.isin ?: "--")
            DataRow("Catalog as of", fund.catalogAsOf)
            Spacer(Modifier.height(Space.md))
            FootnoteText(fund.mandate)
            if (!fund.shariahNotes.isNullOrBlank()) {
                Spacer(Modifier.height(Space.sm))
                FootnoteText(fund.shariahNotes)
            }
        }
    }
}
