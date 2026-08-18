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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.data.repo.FundRow
import app.mizan.android.ui.WatchlistViewModel
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.KeyValueRow
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.SectionCard
import app.mizan.android.ui.components.ShariahBadge

@Composable
fun WatchlistScreen(
    onBack: () -> Unit,
    onOpenFund: (Long) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<FundRow?>(null) }

    editing?.let { row ->
        SipEditorDialog(
            fundName = row.fund.shortName,
            initialAmount = row.sipAmount,
            initialDay = row.sipDayOfMonth,
            onDismiss = { editing = null },
            onSave = { amount, day ->
                viewModel.saveSip(row.fund.schemeCode, amount, day)
                editing = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My watchlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.rows.isEmpty()) {
                item { EmptyState("Add funds from the Funds list.") }
            }

            items(state.rows, key = { it.fund.schemeCode }) { row ->
                SectionCard(
                    title = row.fund.shortName,
                    trailing = { LevelBadge(row.level, row.score) },
                    modifier = Modifier.clickable { onOpenFund(row.fund.schemeCode) },
                ) {
                    Column {
                        KeyValueRow("Scheme", row.fund.schemeCode.toString())
                        KeyValueRow("NAV", Formatters.nav(row.fund.lastNav))
                        KeyValueRow("1M / 1Y", "${Formatters.percentSigned(row.fund.growth1m)} · ${Formatters.percentSigned(row.fund.growth1y)}")
                        KeyValueRow("SIP", Formatters.money(row.sipAmount))
                        KeyValueRow("Debit day", row.sipDayOfMonth?.toString() ?: "--")
                        Spacer(Modifier.height(8.dp))
                        ShariahBadge(row.fund.shariahStatus)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = { editing = row }) { Text("Configure SIP") }
                            TextButton(onClick = { viewModel.remove(row) }) { Text("Remove") }
                        }
                    }
                }
            }

            item {
                FootnoteText(Compliance.SIP_ALLOTMENT_HELPER)
                Spacer(Modifier.height(6.dp))
                FootnoteText(Compliance.DISCLAIMER)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
