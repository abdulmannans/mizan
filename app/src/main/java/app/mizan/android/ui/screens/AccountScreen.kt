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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.ui.AccountViewModel
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.KeyValueRow
import app.mizan.android.ui.components.SectionCard

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "You") {
                    EditableField(
                        label = "Display name",
                        initial = state.settings.displayName,
                        numeric = false,
                        onCommit = { viewModel.setDisplayName(it) },
                    )
                }
            }

            item {
                SectionCard(title = "Sizing") {
                    Column {
                        EditableField(
                            label = "Available lumpsum pool (₹)",
                            initial = "%.0f".format(state.settings.availableLumpsum),
                            numeric = true,
                            onCommit = { value ->
                                value.toDoubleOrNull()?.let(viewModel::setLumpsum)
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        EditableField(
                            label = "What-if lumpsum (₹)",
                            initial = "%.0f".format(state.settings.whatIfAmount),
                            numeric = true,
                            onCommit = { value ->
                                value.toDoubleOrNull()?.let(viewModel::setWhatIf)
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        FootnoteText(
                            "Suggested ₹ is a sizing band from the pool, not a directive. " +
                                Compliance.OVERLAY_NEEDS_CASH
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Notifications") {
                    Column {
                        ToggleRow(
                            "All notifications",
                            state.settings.notificationsEnabled,
                            viewModel::setNotificationsEnabled,
                        )
                        ToggleRow(
                            "Fund dips (65+, watchlist only)",
                            state.settings.notifyFundDips,
                            viewModel::setNotifyFunds,
                        )
                        ToggleRow(
                            "Metal dips (65+)",
                            state.settings.notifyMetalDips,
                            viewModel::setNotifyMetals,
                        )
                        ToggleRow(
                            "Gold ₹10,000 off the 60-day high",
                            state.settings.notifyGoldDrop,
                            viewModel::setNotifyGoldDrop,
                        )
                        if (!state.notificationsAllowed) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Android is blocking notifications for Mizan. Enable them in system " +
                                    "settings; scores still appear in the app either way.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        FootnoteText(
                            "Neutral days (50-64) never notify. Same level inside 7 days stays quiet."
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Background updates") {
                    Column {
                        KeyValueRow("Daily window", "21:00 IST (retry 07:00 IST)")
                        KeyValueRow("Last job", state.lastJob?.name ?: "never run")
                        KeyValueRow("Finished", Formatters.dateTime(state.lastJob?.at))
                        KeyValueRow("Status", state.lastJob?.status ?: "--")
                        KeyValueRow(
                            "Duration",
                            state.lastJob?.durationSeconds?.let { "${it}s" } ?: "--",
                        )
                        KeyValueRow("Funds updated", Formatters.count(state.lastJob?.fundsFetched ?: 0))
                        KeyValueRow("Signals written", Formatters.count(state.lastJob?.signalsWritten ?: 0))
                        KeyValueRow("Notifies posted", Formatters.count(state.lastJob?.notifiesPosted ?: 0))
                        if (state.lastJob?.error != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                state.lastJob?.error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::runUpdateNow) { Text("Run update now") }
                            OutlinedButton(
                                onClick = viewModel::reloadHistory,
                                enabled = !state.backfillRunning,
                            ) {
                                Text(if (state.backfillRunning) "Loading…" else "Reload history")
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Help: background limits") {
                    Column {
                        FootnoteText(
                            "Samsung, Xiaomi and Vivo can kill background jobs. If daily updates " +
                                "stop arriving, allow background activity or autostart for Mizan " +
                                "and exclude it from battery optimisation."
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { requestIgnoreBatteryOptimizations(context) }) {
                            Text("Battery settings")
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Privacy") {
                    Column {
                        FootnoteText(Compliance.PRIVACY)
                        Spacer(Modifier.height(8.dp))
                        FootnoteText(Compliance.DISCLAIMER)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
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
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
