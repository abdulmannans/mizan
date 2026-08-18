package app.mizan.android.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.mizan.android.core.Compliance
import app.mizan.android.ui.OnboardingViewModel
import app.mizan.android.ui.components.FootnoteText
import app.mizan.android.ui.components.SectionCard

/**
 * The disclaimer gate. Acknowledge is mandatory; notifications and battery help are not.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf("") }
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Mizan",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("SIP first. Extra cash on dips.", style = MaterialTheme.typography.titleMedium)

            SectionCard(title = "Before you start") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Compliance.DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
                    FootnoteText(Compliance.AMFI_NAMING)
                    FootnoteText(Compliance.HYPOTHETICAL)
                    FootnoteText(Compliance.PRIVACY)
                }
            }

            SectionCard(title = "Optional") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    FootnoteText(
                        "Daily after market hours, only for real dips (score 65+) and gold drops."
                    )
                    OutlinedButton(
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
                        Text(
                            when {
                                !notificationAsked -> "Allow notifications"
                                notificationGranted -> "Notifications allowed"
                                else -> "Notifications denied — badges only"
                            }
                        )
                    }

                    FootnoteText(
                        "Some phones kill background jobs. Allowing Mizan to ignore battery " +
                            "optimisation keeps the daily update alive."
                    )
                    OutlinedButton(
                        onClick = { requestIgnoreBatteryOptimizations(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Battery settings")
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.createChannels()
                    viewModel.acknowledgeAndStart(displayName)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Acknowledge and continue")
            }

            FootnoteText(
                "Mizan then loads about five years of NAV history so missed dips are not empty. " +
                    "Home stays usable while that runs."
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

fun requestIgnoreBatteryOptimizations(context: android.content.Context) {
    val powerManager = context.getSystemService(PowerManager::class.java)
    val ignoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    val intent = if (ignoring) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
    }
    runCatching { context.startActivity(intent) }
}
