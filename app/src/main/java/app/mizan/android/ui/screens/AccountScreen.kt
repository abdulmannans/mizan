package app.mizan.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.ui.AccountUiState
import app.mizan.android.ui.AccountViewModel
import app.mizan.android.ui.components.DataRow
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.MizanCard
import app.mizan.android.ui.components.SectionHeader
import app.mizan.android.ui.theme.Shapes
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
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
            contentPadding = PaddingValues(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            item { SectionHeader("Sizing") }

            item {
                MizanCard {
                    EditableField(
                        label = "Display name",
                        initial = state.settings.displayName,
                        numeric = false,
                        onCommit = viewModel::setDisplayName,
                    )
                    Spacer(Modifier.height(Space.md))
                    EditableField(
                        label = "Available lumpsum pool (₹)",
                        initial = "%.0f".format(state.settings.availableLumpsum),
                        numeric = true,
                        onCommit = { it.toDoubleOrNull()?.let(viewModel::setLumpsum) },
                    )
                    Spacer(Modifier.height(Space.md))
                    EditableField(
                        label = "What-if lumpsum (₹)",
                        initial = "%.0f".format(state.settings.whatIfAmount),
                        numeric = true,
                        onCommit = { it.toDoubleOrNull()?.let(viewModel::setWhatIf) },
                    )
                    Spacer(Modifier.height(Space.md))
                    FootnoteText(
                        "Suggested ₹ is a sizing band from the pool, not a directive. " +
                            Compliance.OVERLAY_NEEDS_CASH
                    )
                }
            }

            item { SectionHeader("Notifications") }

            item {
                MizanCard {
                    ToggleRow(
                        "All notifications",
                        state.settings.notificationsEnabled,
                        viewModel::setNotificationsEnabled,
                    )
                    ToggleRow(
                        "Fund dips",
                        state.settings.notifyFundDips,
                        viewModel::setNotifyFunds,
                        supporting = "65+, watchlist only",
                    )
                    ToggleRow(
                        "Metal dips",
                        state.settings.notifyMetalDips,
                        viewModel::setNotifyMetals,
                        supporting = "65+",
                    )
                    ToggleRow(
                        "Gold drop",
                        state.settings.notifyGoldDrop,
                        viewModel::setNotifyGoldDrop,
                        supporting = "₹10,000 off the 60-day high",
                    )
                    if (!state.notificationsAllowed) {
                        Spacer(Modifier.height(Space.sm))
                        Text(
                            "Android is blocking notifications for Mizan. Scores still appear in " +
                                "the app either way.",
                            style = TextRole.secondary,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    FootnoteText(
                        "Neutral days (50-64) never notify. The same level inside 7 days stays quiet."
                    )
                }
            }

            item { SectionHeader("Updates") }

            item { UpdatesCard(state, viewModel) }

            item { SectionHeader("Privacy") }

            item {
                MizanCard {
                    FootnoteText(Compliance.PRIVACY)
                    Spacer(Modifier.height(Space.md))
                    OutlinedButton(onClick = { requestIgnoreBatteryOptimizations(context) }) {
                        Text("Battery settings")
                    }
                    Spacer(Modifier.height(Space.sm))
                    FootnoteText(
                        "Samsung, Xiaomi and Vivo can kill background jobs. If daily updates stop " +
                            "arriving, allow background activity for Mizan."
                    )
                }
            }

            item { Spacer(Modifier.height(Space.xl)) }
        }
    }
}

@Composable
private fun UpdatesCard(state: AccountUiState, viewModel: AccountViewModel) {
    var diagnosticsOpen by remember { mutableStateOf(false) }
    MizanCard {
        Text(
            "Daily at 21:00 IST, retried at 07:00 IST.",
            style = TextRole.secondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            "Last run ${Formatters.dateTime(state.lastJob?.at)} · " +
                (state.lastJob?.status ?: "never run"),
            style = TextRole.secondary,
        )
        val error = state.lastJob?.error
        if (error != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                error,
                style = TextRole.secondary,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Button(onClick = viewModel::runUpdateNow) { Text("Run update now") }
            OutlinedButton(
                onClick = viewModel::reloadHistory,
                enabled = !state.backfillRunning,
            ) {
                Text(if (state.backfillRunning) "Loading…" else "Reload history")
            }
        }

        Spacer(Modifier.height(Space.sm))
        HorizontalDivider()
        ListItem(
            modifier = Modifier.clickable { diagnosticsOpen = !diagnosticsOpen },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text("Diagnostics", style = TextRole.body) },
            trailingContent = {
                Icon(
                    if (diagnosticsOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (diagnosticsOpen) "Hide" else "Show",
                )
            },
        )
        AnimatedVisibility(visible = diagnosticsOpen) {
            Column(Modifier.fillMaxWidth()) {
                DataRow("Job", state.lastJob?.name ?: "--")
                DataRow(
                    "Duration",
                    state.lastJob?.durationSeconds?.let { "${it}s" } ?: "--",
                )
                DataRow("Funds updated", Formatters.count(state.lastJob?.fundsFetched ?: 0))
                DataRow("Signals written", Formatters.count(state.lastJob?.signalsWritten ?: 0))
                DataRow("Notifies posted", Formatters.count(state.lastJob?.notifiesPosted ?: 0))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(label, style = TextRole.body) },
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    style = TextRole.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun EditableField(
    label: String,
    initial: String,
    numeric: Boolean,
    onCommit: (String) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    OutlinedTextField(
        value = value,
        onValueChange = {
            value = it
            onCommit(it)
        },
        label = { Text(label) },
        singleLine = true,
        shape = Shapes.field,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
