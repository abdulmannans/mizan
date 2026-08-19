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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.data.repo.MetalDetailState
import app.mizan.android.domain.PricePoint
import app.mizan.android.ui.MetalDetailViewModel
import app.mizan.android.ui.MetalsViewModel
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
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

@Composable
fun MetalsScreen(
    onOpenMetal: (String) -> Unit,
    viewModel: MetalsViewModel = hiltViewModel(),
) {
    val rows by viewModel.state.collectAsState()
    var showDisclaimer by remember { mutableStateOf(false) }

    if (showDisclaimer) DisclaimerDialog { showDisclaimer = false }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        item {
            Column {
                Text("Metals", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "The other sleeve. Metal alerts are independent of the dip score.",
                    style = TextRole.secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { ComplianceChip(onRead = { showDisclaimer = true }) }

        item {
            MizanCard {
                rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider()
                    EntityRow(
                        title = row.metal.name,
                        subtitle = "${Formatters.money(row.metal.lastPrice)} ${row.metal.unit} · " +
                            "1D ${Formatters.percentSigned(row.metal.growth1d)} · " +
                            "1Y ${Formatters.percentSigned(row.metal.growth1y)}",
                        onClick = { onOpenMetal(row.metal.id) },
                        trailing = { LevelBadge(row.level, row.score) },
                    )
                }
                if (rows.isEmpty()) EmptyState("Prices arrive with the first update.")
            }
        }

        item {
            FootnoteText(Compliance.METAL_QUOTES)
            Spacer(Modifier.height(Space.xl))
        }
    }
}

@Composable
fun MetalDetailScreen(
    onBack: () -> Unit,
    viewModel: MetalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val metal = state.metal
    var showDisclaimer by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(DetailPane.OVERVIEW) }

    if (showDisclaimer) DisclaimerDialog { showDisclaimer = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(metal?.name ?: "Metal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (metal == null) {
            EmptyState("Metal not found.", Modifier.padding(padding).padding(Space.lg))
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Formatters.money(metal.lastPrice), style = TextRole.figure)
                        Spacer(Modifier.width(Space.md))
                        LevelBadge(
                            state.signals.firstOrNull()?.level,
                            state.signals.firstOrNull()?.score,
                        )
                    }
                    Spacer(Modifier.height(Space.xs))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1D ", style = TextRole.caption)
                        GrowthText(metal.growth1d)
                        Text("   1M ", style = TextRole.caption)
                        GrowthText(metal.growth1m)
                        Text("   1Y ", style = TextRole.caption)
                        GrowthText(metal.growth1y)
                    }
                    Text(
                        "${metal.unit} · as of ${Formatters.date(metal.priceAsOf)}",
                        style = TextRole.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                LineChart(
                    points = state.priceSeries,
                    valueFormatter = { Formatters.money(it) },
                )
            }

            item { DetailPaneSelector(selected = tab, onSelect = { tab = it }) }

            when (tab) {
                DetailPane.OVERVIEW -> metalOverviewTab(state)
                DetailPane.SCORE -> metalScoreTab(state)
                DetailPane.HISTORY -> metalHistoryTab(state)
            }

            item {
                ComplianceChip(onRead = { showDisclaimer = true })
                Spacer(Modifier.height(Space.sm))
                FootnoteText(Compliance.METAL_QUOTES)
                Spacer(Modifier.height(Space.xl))
            }
        }
    }
}

private fun LazyListScope.metalOverviewTab(state: MetalDetailState) {
    item {
        MizanCard(title = "Off recent high") {
            DataRow("60-day peak", Formatters.money(state.peak))
            DataRow("Peak date", Formatters.date(state.peakDate))
            DataRow("Drop", Formatters.money(state.dropRupees))
            DataRow(
                "Alert threshold",
                if (state.thresholdRupees > 0) Formatters.money(state.thresholdRupees) else "Off",
            )
            Spacer(Modifier.height(Space.sm))
            FootnoteText(
                "A plain rupee fall from the recent peak, independent of the dip score."
            )
        }
    }

    item {
        val latest = state.signals.firstOrNull()
        MizanCard(title = "Dip opportunity") {
            if (latest == null) {
                EmptyState("Not scored yet.")
            } else {
                Text(
                    "Scored ${Formatters.date(latest.date)} at " +
                        Formatters.money(latest.value),
                    style = TextRole.secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.sm))
                latest.reasons.forEach { reason ->
                    Text("• $reason", style = TextRole.secondary)
                }
            }
        }
    }
}

private fun LazyListScope.metalScoreTab(state: MetalDetailState) {
    item {
        val latest = state.signals.firstOrNull()
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

private fun LazyListScope.metalHistoryTab(state: MetalDetailState) {
    if (state.signals.isEmpty()) {
        item { EmptyState("No scores yet.") }
        return
    }

    items(state.signals.take(60), key = { it.date.toEpochDay() }) { signal ->
        EntityRow(
            title = Formatters.date(signal.date),
            subtitle = Formatters.money(signal.value),
            trailing = { LevelBadge(signal.level, signal.score) },
        )
        HorizontalDivider()
    }
}
