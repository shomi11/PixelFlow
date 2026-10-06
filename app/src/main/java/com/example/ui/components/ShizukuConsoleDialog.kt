package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShellResult
import com.example.shizuku.ShizukuState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShizukuConsoleDialog(
    shizukuState: ShizukuState,
    shizukuVersion: Int,
    lastResult: ShellResult?,
    onRequestPermission: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var commandInput by remember { mutableStateOf("settings get secure doze_always_on") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = "Console",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shizuku Shell Console",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (shizukuState) {
                            ShizukuState.AUTHORIZED -> MaterialTheme.colorScheme.primaryContainer
                            ShizukuState.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiaryContainer
                            ShizukuState.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (shizukuState) {
                                ShizukuState.AUTHORIZED -> Icons.Default.CheckCircle
                                ShizukuState.PERMISSION_REQUIRED -> Icons.Default.Error
                                ShizukuState.UNAVAILABLE -> Icons.Default.Close
                            },
                            contentDescription = null,
                            tint = when (shizukuState) {
                                ShizukuState.AUTHORIZED -> MaterialTheme.colorScheme.primary
                                ShizukuState.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                                ShizukuState.UNAVAILABLE -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (shizukuState) {
                                    ShizukuState.AUTHORIZED -> "Connected & Authorized (v$shizukuVersion)"
                                    ShizukuState.PERMISSION_REQUIRED -> "Binder Available - Permission Required"
                                    ShizukuState.UNAVAILABLE -> "Shizuku Service Not Running (Simulation Mode)"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (shizukuState == ShizukuState.PERMISSION_REQUIRED) {
                            FilledTonalButton(
                                onClick = onRequestPermission,
                                modifier = Modifier.testTag("dialog_grant_shizuku_btn")
                            ) {
                                Text("Grant", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Text(
                    text = "Quick Command Presets",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "Query AOD" to "settings get secure doze_always_on",
                        "Toggle AOD On" to "settings put secure doze_always_on 1",
                        "Toggle AOD Off" to "settings put secure doze_always_on 0",
                        "Dark Mode Yes" to "cmd uimode night yes",
                        "Dark Mode No" to "cmd uimode night no",
                        "Battery Mode" to "cmd power get-mode",
                        "Screen Timeout" to "settings get system screen_off_timeout"
                    )

                    presets.forEach { (label, cmd) ->
                        SuggestionChip(
                            onClick = {
                                commandInput = cmd
                                onExecuteCommand(cmd)
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                // Input field
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    label = { Text("Command") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { onExecuteCommand(commandInput) },
                            modifier = Modifier.testTag("run_console_cmd_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onExecuteCommand(commandInput) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("console_cmd_input")
                )

                // Shell Result Output Window
                if (lastResult != null) {
                    Text(
                        text = "Output (Exit code: ${lastResult.exitCode})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E24))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            Text(
                                text = "$ ${lastResult.executedCommand}",
                                color = Color(0xFF81D4FA),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (lastResult.stdout.isNotBlank()) {
                                Text(
                                    text = lastResult.stdout,
                                    color = Color(0xFFA5D6A7),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                            if (lastResult.stderr.isNotBlank()) {
                                Text(
                                    text = lastResult.stderr,
                                    color = Color(0xFFEF9A9A),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExecuteCommand(commandInput) },
                modifier = Modifier.testTag("console_execute_button")
            ) {
                Text("Execute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
