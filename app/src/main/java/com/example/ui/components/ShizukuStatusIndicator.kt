package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.NetworkStatusMonitor
import com.example.shizuku.ShizukuState

/**
 * ShizukuStatusIndicator checks for active Shizuku permissions and displays
 * a high-visibility, persistent warning when unauthorized or disconnected,
 * complete with direct action buttons and direct navigation to the setup guide.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShizukuStatusIndicator(
    state: ShizukuState,
    version: Int,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onOpenConsole: (() -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
    isWifiConnected: Boolean = true,
    networkDescription: String = "",
    isReconnecting: Boolean = false,
    onReconnect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedCommand by remember { mutableStateOf(false) }

    when (state) {
        ShizukuState.AUTHORIZED -> {
            // Authorized state: Reassuring, polished confirmation card
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("shizuku_status_authorized_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Authorized",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Shizuku ADB Active",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "API v$version",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Text(
                                    text = "Privileged system commands & AOD controls are fully operational",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("shizuku_refresh_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                        }
                    }

                    if (onOpenConsole != null || onOpenDiagnostics != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (onOpenDiagnostics != null) {
                                OutlinedButton(
                                    onClick = onOpenDiagnostics,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("banner_diagnostics_btn")
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Diagnostics")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (onOpenConsole != null) {
                                OutlinedButton(
                                    onClick = onOpenConsole,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("banner_console_btn")
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shell Terminal")
                                }
                            }
                        }
                    }
                }
            }
        }

        ShizukuState.PERMISSION_REQUIRED -> {
            // Persistent Warning: Shizuku daemon is running but needs user permission
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("shizuku_status_permission_required_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0),
                    contentColor = Color(0xFF5D4037)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB74D))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF57C00).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Permission Required",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Required: Shizuku Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB71C1C)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Shizuku service is running on your device, but Pixel Routines does not have permission to execute ADB shell commands. Automated rules cannot modify system settings until permission is granted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4E342E)
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("shizuku_refresh_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFE65100))
                        }
                    }

                    // Action Buttons FlowRow
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE65100),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("shizuku_grant_permission_btn")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (onReconnect != null) onReconnect() else onRefresh()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE65100)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE65100)),
                            modifier = Modifier.testTag("shizuku_perm_reconnect_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isReconnecting) "Reconnecting..." else "Reconnect")
                        }

                        if (onOpenDiagnostics != null) {
                            OutlinedButton(
                                onClick = onOpenDiagnostics,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFE65100)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE65100)),
                                modifier = Modifier.testTag("shizuku_perm_diag_btn")
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diagnostics")
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenSetupGuide,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE65100)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE65100)),
                            modifier = Modifier.testTag("shizuku_setup_guide_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setup Guide")
                        }
                    }
                }
            }
        }

        ShizukuState.UNAVAILABLE -> {
            // Persistent Warning: Shizuku daemon is inactive, stopped, or not installed
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("shizuku_status_unavailable_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Disconnected",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Persistent Warning: Shizuku Not Running",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "The Shizuku binder daemon is inactive or offline. Routines are currently unable to apply system settings (AOD, Auto-Rotate, Dark Theme, Battery Saver) directly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("shizuku_refresh_status_btn")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Quick ADB Command Box with Copy Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1E24))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = Color(0xFF81C995),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(
                                        AnnotatedString("adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh")
                                    )
                                    copiedCommand = true
                                    Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("copy_adb_command_btn")
                            ) {
                                Icon(
                                    imageVector = if (copiedCommand) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy Command",
                                    tint = if (copiedCommand) Color(0xFF81C995) else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Wi-Fi Disconnected Warning Banner (explains why Shizuku stops working outside home)
                    if (!isWifiConnected) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shizuku_wifi_disconnected_banner"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Left Home Wi-Fi ($networkDescription)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Android OS automatically terminates the Wireless Debugging port when you disconnect from Wi-Fi. Shizuku cannot run over mobile cellular data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "How to use Shizuku on the go / outside:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "• Turn on Personal Hotspot to create a local Wi-Fi port on the go.\n" +
                                            "• Set Shizuku Battery to 'Unrestricted' so the daemon survives.\n" +
                                            "• When back home, reconnect to Wi-Fi and tap Re-check.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { NetworkStatusMonitor.openHotspotSettings(context) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier.testTag("banner_hotspot_btn")
                                    ) {
                                        Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Hotspot", fontSize = 11.5.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { NetworkStatusMonitor.openShizukuAppDetails(context) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier.testTag("banner_battery_btn")
                                    ) {
                                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Unrestricted Battery", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Direct Link Action Buttons
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Reconnect / Re-bind to Shizuku Service
                        Button(
                            onClick = {
                                if (onReconnect != null) {
                                    onReconnect()
                                } else {
                                    onRefresh()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("shizuku_reconnect_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isReconnecting) "Reconnecting..." else "Reconnect Shizuku", fontWeight = FontWeight.Bold)
                        }

                        // 2. Connect & Authorize (attempts connection and dispatches permission prompt)
                        OutlinedButton(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("shizuku_connect_btn")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant Permission")
                        }

                        // 2. Open Wireless Debugging Settings directly
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        try {
                                            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                                        } catch (e3: Exception) {
                                            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("shizuku_wireless_debugging_btn")
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wireless Debugging Settings")
                        }

                        // 3. Launch Shizuku App directly if installed
                        OutlinedButton(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Shizuku app not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("shizuku_launch_app_btn")
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Shizuku App")
                        }

                        // 4. Re-check / Refresh status
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("shizuku_check_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-check Status")
                        }

                        // 5. Diagnostics & Troubleshooting
                        if (onOpenDiagnostics != null) {
                            OutlinedButton(
                                onClick = onOpenDiagnostics,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("shizuku_unavail_diag_btn")
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diagnostics")
                            }
                        }

                        // 6. Direct Link to Setup Guide
                        OutlinedButton(
                            onClick = onOpenSetupGuide,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("shizuku_setup_guide_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setup Guide")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact pill-shaped chip indicator for placement in app bars or quick headers.
 */
@Composable
fun ShizukuStatusChip(
    state: ShizukuState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (dotColor, label) = when (state) {
        ShizukuState.AUTHORIZED -> Color(0xFF2E7D32) to "Shizuku Active"
        ShizukuState.PERMISSION_REQUIRED -> Color(0xFFF57C00) to "Needs Permission"
        ShizukuState.UNAVAILABLE -> Color(0xFFD32F2F) to "Shizuku Offline"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = dotColor.copy(alpha = 0.12f),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("shizuku_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = dotColor
            )
        }
    }
}
