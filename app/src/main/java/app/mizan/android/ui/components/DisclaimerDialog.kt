package app.mizan.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.mizan.android.core.Compliance

@Composable
fun DisclaimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Important") },
        text = {
            Column {
                Text(Compliance.DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text(Compliance.AMFI_NAMING, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text(Compliance.HYPOTHETICAL, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text(Compliance.METAL_QUOTES, style = MaterialTheme.typography.bodySmall)
            }
        },
    )
}
