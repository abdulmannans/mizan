package app.mizan.android.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.mizan.android.core.Formatters
import app.mizan.android.data.repo.FundRow
import app.mizan.android.ui.WatchlistViewModel
import app.mizan.android.ui.components.EmptyState
import app.mizan.android.ui.components.EntityRow
import app.mizan.android.ui.components.LevelBadge
import app.mizan.android.ui.components.ShariahBadge
import app.mizan.android.ui.theme.Space

@Composable
fun WatchlistScreen(
    onBack: () -> Unit,
    onOpenFund: (Long) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
            contentPadding = PaddingValues(bottom = Space.xl),
        ) {
            if (state.rows.isEmpty()) {
                item {
                    EmptyState("Add funds from the Funds list.", Modifier.padding(Space.lg))
                }
            }

            items(state.rows, key = { it.fund.schemeCode }) { row ->
                WatchlistRow(
                    row = row,
                    onOpen = { onOpenFund(row.fund.schemeCode) },
                    onRemove = { viewModel.remove(row) },
                )
                HorizontalDivider()
            }

            item { Spacer(Modifier.height(Space.xl)) }
        }
    }
}

@Composable
private fun WatchlistRow(
    row: FundRow,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    EntityRow(
        title = row.fund.shortName,
        overlineContent = { ShariahBadge(row.fund.shariahStatus) },
        subtitle = buildString {
            append("NAV ${Formatters.nav(row.fund.lastNav)}")
            append("   1M ${Formatters.percentSigned(row.fund.growth1m)}")
            append("   1Y ${Formatters.percentSigned(row.fund.growth1y)}")
        },
        onClick = onOpen,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(row.level, row.score)
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                }
            }
        },
    )
}
