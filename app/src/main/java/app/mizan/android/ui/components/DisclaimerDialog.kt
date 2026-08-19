package app.mizan.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.mizan.android.core.Compliance
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

@Composable
fun DisclaimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Important") },
        text = {
            Column {
                Text(Compliance.DISCLAIMER, style = TextRole.body)
                Spacer(Modifier.height(Space.md))
                Text(Compliance.AMFI_NAMING, style = TextRole.secondary)
                Spacer(Modifier.height(Space.md))
                Text(Compliance.HYPOTHETICAL, style = TextRole.secondary)
                Spacer(Modifier.height(Space.md))
                Text(Compliance.METAL_QUOTES, style = TextRole.secondary)
            }
        },
    )
}
