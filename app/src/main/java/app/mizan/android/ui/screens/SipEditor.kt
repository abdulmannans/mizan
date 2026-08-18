package app.mizan.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.mizan.android.core.Compliance

private const val MIN_SIP_AMOUNT = 100.0

@Composable
fun SipEditorDialog(
    fundName: String,
    initialAmount: Double?,
    initialDay: Int?,
    onDismiss: () -> Unit,
    onSave: (Double?, Int?) -> Unit,
) {
    var amount by remember { mutableStateOf(initialAmount?.let { "%.0f".format(it) } ?: "") }
    var day by remember { mutableStateOf(initialDay?.toString() ?: "") }

    val parsedAmount = amount.trim().toDoubleOrNull()
    val parsedDay = day.trim().toIntOrNull()
    val amountError = amount.isNotBlank() && (parsedAmount == null || parsedAmount < MIN_SIP_AMOUNT)
    val dayError = day.isNotBlank() && (parsedDay == null || parsedDay !in 1..28)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SIP for $fundName") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monthly amount (₹, min 100)") },
                    isError = amountError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = day,
                    onValueChange = { day = it },
                    label = { Text("Debit day (1-28)") },
                    isError = dayError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    Compliance.SIP_ALLOTMENT_HELPER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Mizan never asks you to pause a SIP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !amountError && !dayError,
                onClick = { onSave(parsedAmount, parsedDay) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
