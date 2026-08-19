package app.mizan.android.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.ui.OnboardingViewModel
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.MizanCard
import app.mizan.android.ui.theme.Shapes
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf("") }
    var notifyFundDips by remember { mutableStateOf(true) }
    var notifyMetalDips by remember { mutableStateOf(true) }
    var notifyGoldDrop by remember { mutableStateOf(true) }
    var notificationAsked by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationAsked = true
        notificationGranted = granted
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "Mizan",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Market dip alerts for smart investing.", style = TextRole.title)

            MizanCard(title = "Before you start") {
                Text(
                    listOf(
                        Compliance.DISCLAIMER,
                        Compliance.AMFI_NAMING,
                        Compliance.HYPOTHETICAL,
                        Compliance.PRIVACY,
                    ).joinToString("\n\n"),
                    style = TextRole.body,
                )
            }

            MizanCard(title = "Your details") {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        shape = Shapes.field,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            MizanCard(title = "Notifications") {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    FootnoteText(
                        "A quiet daily summary after market hours. " +
                            "Loud alerts only when a fund scores 65+ or gold drops hard."
                    )
                    Spacer(Modifier.height(Space.xs))

                    NotificationToggle("Fund dip alerts", notifyFundDips) { notifyFundDips = it }
                    NotificationToggle("Metal dip alerts", notifyMetalDips) { notifyMetalDips = it }
                    NotificationToggle("Gold drop alerts", notifyGoldDrop) { notifyGoldDrop = it }

                    Spacer(Modifier.height(Space.sm))

                    if (!notificationAsked) {
                        Button(
                            onClick = {
                                viewModel.createChannels()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationAsked = true
                                    notificationGranted = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Allow notifications")
                        }
                    } else {
                        FootnoteText(
                            if (notificationGranted) "Notifications allowed."
                            else "Notifications denied. You can enable them in system settings."
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.createChannels()
                    viewModel.acknowledgeAndStart(
                        displayName,
                        notifyFundDips,
                        notifyMetalDips,
                        notifyGoldDrop,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Acknowledge and continue")
            }

            FootnoteText(
                "Mizan then loads about five years of NAV history so missed dips are not empty. " +
                    "Home stays usable while that runs."
            )
            Spacer(Modifier.height(Space.xl))
        }
    }
}

fun requestIgnoreBatteryOptimizations(context: android.content.Context) {
    val powerManager = context.getSystemService(android.os.PowerManager::class.java)
    val ignoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    val intent = if (ignoring) {
        android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        android.content.Intent(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            android.net.Uri.parse("package:${context.packageName}"),
        )
    }
    runCatching { context.startActivity(intent) }
}

@Composable
private fun NotificationToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = TextRole.body)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
