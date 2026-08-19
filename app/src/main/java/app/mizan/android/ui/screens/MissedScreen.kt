package app.mizan.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.mizan.android.domain.MissedDeploy
import app.mizan.android.ui.MissedViewModel
import app.mizan.android.ui.components.ComplianceChip
import app.mizan.android.ui.components.DataRow
import app.mizan.android.ui.components.DisclaimerDialog
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.EntityRow
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.MizanCard
import app.mizan.android.ui.components.SectionHeader
import app.mizan.android.ui.components.Stat
import app.mizan.android.ui.components.StatStrip
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

@Composable
fun MissedScreen(
    onOpenFund: (Long) -> Unit,
    viewModel: MissedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val totals = state.totals
    var showDisclaimer by remember { mutableStateOf(false) }

    if (showDisclaimer) DisclaimerDialog { showDisclaimer = false }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        item {
            Column {
                Text("Missed opportunities", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Attractive days (65+) on your watchlist, clustered so one crash counts once.",
                    style = TextRole.secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { ComplianceChip(onRead = { showDisclaimer = true }) }

        item {
            MizanCard(title = "Suggested-₹ scenario") {
                StatStrip(
                    listOf(
                        Stat("Deploys", Formatters.count(totals.deploys)),
                        Stat("Invested", Formatters.money(totals.suggestedInvested)),
                        Stat("Value today", Formatters.money(totals.suggestedValueToday)),
                        Stat(
                            "P&L",
                            Formatters.moneySigned(totals.suggestedPnl),
                            Formatters.percentSigned(totals.suggestedPnlPercent),
                        ),
                    )
                )
                Spacer(Modifier.height(Space.md))
                HorizontalDivider()
                Spacer(Modifier.height(Space.md))
                Text(
                    "If ${Formatters.money(state.settings.whatIfAmount)} each",
                    style = TextRole.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.sm))
                StatStrip(
                    listOf(
                        Stat("Invested", Formatters.money(totals.whatIfInvested)),
                        Stat("Value today", Formatters.money(totals.whatIfValueToday)),
                        Stat(
                            "P&L",
                            Formatters.moneySigned(totals.whatIfPnl),
                            Formatters.percentSigned(totals.whatIfPnlPercent),
                        ),
                    )
                )
                Spacer(Modifier.height(Space.md))
                FootnoteText(Compliance.HYPOTHETICAL)
            }
        }

        item { SectionHeader("Today") }

        item {
            if (state.watchlistEmpty) {
                EmptyState("Add funds from the Funds list.")
            } else {
                MizanCard {
                    state.today.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider()
                        EntityRow(
                            title = row.fund.shortName,
                            subtitle = if (row.attractiveNow) {
                                "Attractive · suggested " +
                                    Formatters.money(
                                        row.suggestedRupees(state.settings.availableLumpsum)
                                    )
                            } else {
                                "Not attractive now"
                            },
                            onClick = { onOpenFund(row.fund.schemeCode) },
                            trailing = { LevelBadge(row.level, row.score) },
                        )
                    }
                }
            }
        }

        item { SectionHeader("Missed dips") }

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
                DeployRow(deploy = deploy, onOpenFund = onOpenFund)
            }
        }

        item { Spacer(Modifier.height(Space.xl)) }
    }
}

/** One line per missed dip; the six figures only appear when asked for. */
@Composable
private fun DeployRow(deploy: MissedDeploy, onOpenFund: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    MizanCard {
        EntityRow(
            title = deploy.name,
            overline = Formatters.date(deploy.date),
            subtitle = "${Formatters.money(deploy.suggestedRupees)} → " +
                "${Formatters.money(deploy.valueToday)} " +
                "(${Formatters.percentSigned(deploy.returnPercent)})",
            onClick = { expanded = !expanded },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelBadge(deploy.level, deploy.score)
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            },
        )
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth()) {
                DataRow(
                    "NAV then → now",
                    "${Formatters.nav(deploy.navThen)} → ${Formatters.nav(deploy.navNow)}",
                )
                DataRow(
                    "If ${Formatters.money(deploy.whatIfInvested)}",
                    "${Formatters.money(deploy.whatIfValueToday)} " +
                        "(${Formatters.moneySigned(deploy.whatIfPnl)})",
                )
                TextButton(
                    onClick = { deploy.targetId.toLongOrNull()?.let(onOpenFund) },
                    modifier = Modifier.padding(top = Space.xs),
                ) {
                    Text("Open fund")
                }
            }
        }
    }
}
