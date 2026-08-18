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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import app.mizan.android.domain.AllocationBands
import app.mizan.android.domain.PricePoint
import app.mizan.android.ui.FundDetailViewModel
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.GrowthText
import app.mizan.android.ui.components.KeyValueRow
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.LineChart
import app.mizan.android.ui.components.SectionCard
import app.mizan.android.ui.components.ShariahBadge

@Composable
fun FundDetailScreen(
    onBack: () -> Unit,
    viewModel: FundDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fund = state.fund
    var showSipEditor by remember { mutableStateOf(false) }

    if (showSipEditor && fund != null) {
        SipEditorDialog(
            fundName = fund.shortName,
            initialAmount = state.sipAmount,
            initialDay = state.sipDayOfMonth,
            onDismiss = { showSipEditor = false },
            onSave = { amount, day ->
                viewModel.saveSip(amount, day)
                showSipEditor = false
            },
        )
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
                    IconButton(onClick = { viewModel.toggleWatchlist(state.watchlisted) }) {
                        Icon(
                            imageVector = if (state.watchlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (state.watchlisted) "Remove from watchlist" else "Add to watchlist",
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
            EmptyState("Fund not found.", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    FootnoteText(Compliance.DISCLAIMER)
                }
            }

            item {
                SectionCard(title = "Identity") {
                    Column {
                        Text(fund.name, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow("Scheme code", fund.schemeCode.toString())
                        KeyValueRow("AMC", fund.amc)
                        KeyValueRow("ISIN", fund.isin ?: "--")
                        KeyValueRow("Catalog as of", fund.catalogAsOf)
                        Spacer(Modifier.height(8.dp))
                        ShariahBadge(fund.shariahStatus)
                        Spacer(Modifier.height(8.dp))
                        FootnoteText(fund.mandate)
                        if (!fund.shariahNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            FootnoteText(fund.shariahNotes)
                        }
                    }
                }
            }

            item {
                SectionCard(title = "NAV and growth") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Formatters.nav(fund.lastNav),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row {
                                    Text("1D ", style = MaterialTheme.typography.bodySmall)
                                    GrowthText(fund.growth1d)
                                    Text("  1M ", style = MaterialTheme.typography.bodySmall)
                                    GrowthText(fund.growth1m)
                                    Text("  1Y ", style = MaterialTheme.typography.bodySmall)
                                    GrowthText(fund.growth1y)
                                }
                                Text(
                                    "as of ${Formatters.date(fund.navAsOf)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LineChart(points = state.navSeries)
                    }
                }
            }

            item {
                val latest = state.latest
                SectionCard(
                    title = "Dip opportunity",
                    trailing = { LevelBadge(latest?.level, latest?.score) },
                ) {
                    if (latest == null) {
                        EmptyState("Not scored yet.")
                    } else {
                        Column {
                            KeyValueRow("Score date", Formatters.date(latest.date))
                            KeyValueRow("NAV at score", Formatters.nav(latest.value))
                            KeyValueRow(
                                "Suggested extra lumpsum",
                                "${AllocationBands.suggestedPercent(latest.score)}% · " +
                                    Formatters.money(
                                        AllocationBands.suggestedRupees(
                                            latest.score,
                                            state.settings.availableLumpsum,
                                        )
                                    ),
                            )
                            Spacer(Modifier.height(10.dp))
                            latest.components.labelled().forEach { (label, value, weight) ->
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(label, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "$value / $weight",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = {
                                            (value.toFloat() / weight).coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            latest.reasons.forEach { reason ->
                                Text("• $reason", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(10.dp))
                            FootnoteText(Compliance.OVERLAY_NEEDS_CASH)
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Score history") {
                    LineChart(
                        points = state.signals
                            .sortedBy { it.date }
                            .map { PricePoint(it.date, it.score.toDouble()) },
                        valueFormatter = { "%.0f".format(it) },
                        fixedRange = 0f..100f,
                    )
                }
            }

            item {
                SectionCard(
                    title = "Watchlist and SIP",
                    trailing = {
                        OutlinedButton(onClick = { showSipEditor = true }) { Text("Configure") }
                    },
                ) {
                    Column {
                        KeyValueRow("On watchlist", if (state.watchlisted) "Yes" else "No")
                        KeyValueRow("SIP amount", Formatters.money(state.sipAmount))
                        KeyValueRow("Debit day", state.sipDayOfMonth?.toString() ?: "--")
                        val sip = state.nextSip
                        if (sip != null) {
                            KeyValueRow("Next debit", Formatters.date(sip.debitDate))
                            KeyValueRow("Estimated allotment", Formatters.date(sip.allotmentDate))
                            KeyValueRow(
                                "Allotment NAV vs chart NAV",
                                "${Formatters.nav(sip.estimatedAllotmentNav)} vs ${Formatters.nav(sip.latestNav)}",
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        FootnoteText(Compliance.SIP_ALLOTMENT_HELPER)
                    }
                }
            }

            item { Text("Signals history", style = MaterialTheme.typography.titleMedium) }

            items(state.signals.take(SIGNAL_PAGE_SIZE), key = { it.date.toEpochDay() }) { signal ->
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(Formatters.date(signal.date), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "NAV ${Formatters.nav(signal.value)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LevelBadge(signal.level, signal.score)
                    }
                    Text(
                        signal.reasons.firstOrNull().orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }

            item {
                if (state.signals.size > SIGNAL_PAGE_SIZE) {
                    FootnoteText("Showing the latest $SIGNAL_PAGE_SIZE of ${state.signals.size} scores.")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private const val SIGNAL_PAGE_SIZE = 60
