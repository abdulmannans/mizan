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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.domain.PricePoint
import app.mizan.android.ui.MetalDetailViewModel
import app.mizan.android.ui.MetalsViewModel
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.GrowthText
import app.mizan.android.ui.components.KeyValueRow
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.LineChart
import app.mizan.android.ui.components.SectionCard

@Composable
fun MetalsScreen(
    onOpenMetal: (String) -> Unit,
    viewModel: MetalsViewModel = hiltViewModel(),
) {
    val rows by viewModel.state.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Metals", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "The other sleeve. Metal alerts are independent of the 0-100 dip score.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(rows, key = { it.metal.id }) { row ->
            SectionCard(
                title = row.metal.name,
                trailing = { LevelBadge(row.level, row.score) },
                modifier = Modifier.clickable { onOpenMetal(row.metal.id) },
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Formatters.money(row.metal.lastPrice),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        row.metal.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("1D ", style = MaterialTheme.typography.bodySmall)
                        GrowthText(row.metal.growth1d)
                        Text("  1M ", style = MaterialTheme.typography.bodySmall)
                        GrowthText(row.metal.growth1m)
                        Text("  1Y ", style = MaterialTheme.typography.bodySmall)
                        GrowthText(row.metal.growth1y)
                    }
                    Text(
                        "as of ${Formatters.date(row.metal.priceAsOf)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            FootnoteText(Compliance.METAL_QUOTES)
            Spacer(Modifier.height(6.dp))
            FootnoteText(Compliance.DISCLAIMER)
            Spacer(Modifier.height(24.dp))
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
            EmptyState("Metal not found.", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "Price") {
                    Column {
                        Text(
                            Formatters.money(metal.lastPrice),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            "${metal.unit} · as of ${Formatters.date(metal.priceAsOf)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        LineChart(points = state.priceSeries, valueFormatter = { Formatters.money(it) })
                    }
                }
            }

            item {
                SectionCard(title = "Off recent high") {
                    Column {
                        KeyValueRow("60-day peak", Formatters.money(state.peak))
                        KeyValueRow("Peak date", Formatters.date(state.peakDate))
                        KeyValueRow("Drop", Formatters.money(state.dropRupees))
                        KeyValueRow(
                            "Alert threshold",
                            if (state.thresholdRupees > 0) {
                                Formatters.money(state.thresholdRupees)
                            } else {
                                "Off"
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        FootnoteText(
                            "This card is independent of the dip score: it is a plain rupee fall " +
                                "from the recent peak."
                        )
                    }
                }
            }

            item {
                SectionCard(
                    title = "Dip opportunity",
                    trailing = {
                        LevelBadge(state.signals.firstOrNull()?.level, state.signals.firstOrNull()?.score)
                    },
                ) {
                    val latest = state.signals.firstOrNull()
                    if (latest == null) {
                        EmptyState("Not scored yet.")
                    } else {
                        Column {
                            KeyValueRow("Score date", Formatters.date(latest.date))
                            KeyValueRow("Price at score", Formatters.money(latest.value))
                            Spacer(Modifier.height(8.dp))
                            latest.reasons.forEach { reason ->
                                Text("• $reason", style = MaterialTheme.typography.bodySmall)
                            }
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

            item { Text("Signals history", style = MaterialTheme.typography.titleMedium) }

            items(state.signals.take(60), key = { it.date.toEpochDay() }) { signal ->
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(Formatters.date(signal.date), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Formatters.money(signal.value),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LevelBadge(signal.level, signal.score)
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }

            item {
                FootnoteText(Compliance.METAL_QUOTES)
                Spacer(Modifier.height(6.dp))
                FootnoteText(Compliance.DISCLAIMER)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
