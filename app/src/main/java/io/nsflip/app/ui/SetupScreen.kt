// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.nsflip.app.shizuku.GrantManager
import kotlinx.coroutines.launch

private const val ADB_COMMAND =
    "adb shell pm grant io.nsflip.app ${GrantManager.PERMISSION}"
private const val SHIZUKU_PLAY_URL = "https://shizuku.rikka.app/"

/**
 * One-time setup checklist: grant WRITE_SECURE_SETTINGS via Shizuku (one tap)
 * or a copy-paste adb command.
 */
@Composable
fun SetupScreen(
    status: GrantManager.Status,
    onRefresh: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Setup", style = MaterialTheme.typography.displaySmall)
            Text(
                "NSFlip changes the Private DNS system setting. Android requires a one-time privilege grant. No root needed.",
                style = MaterialTheme.typography.bodyMedium,
            )

            CheckRow("1. Install Shizuku", status.shizukuInstalled)
            CheckRow("2. Shizuku is running (start it once)", status.shizukuRunning)
            CheckRow("3. Shizuku permission granted", status.shizukuPermission)
            CheckRow("4. WRITE_SECURE_SETTINGS granted ✓", status.granted)

            val step = when {
                status.granted -> null
                !status.shizukuRunning -> "Open Shizuku and start it (wireless or USB), then come back."
                !status.shizukuPermission -> null
                else -> "Tap Grant now."
            }
            step?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            when {
                status.granted -> {}
                status.shizukuRunning && status.shizukuPermission -> {
                    Button(
                        onClick = {
                            scope.launch {
                                val err = GrantManager.grantSelf(context)
                                if (err != null) onError(err) else onRefresh()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Grant one-tap") }
                }
                status.shizukuRunning -> {
                    Button(
                        onClick = {
                            GrantManager.requestShizukuPermission { _ -> onRefresh() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Allow Shizuku access") }
                }
                !status.shizukuInstalled -> {
                    OutlinedButton(
                        onClick = { Open.link(context, SHIZUKU_PLAY_URL) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Get Shizuku (free, open source)") }
                }
            }

            Text(
                "No Shizuku? Grant over USB with adb (Developer options → USB debugging), then reopen NSFlip:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ADB_COMMAND,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                IconButton(
                    onClick = {
                        context.clipboard(ClipData.newPlainText("nsflip", ADB_COMMAND))
                    },
                ) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy adb command") }
            }

            TextButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("  Re-check state")
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = done,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun Context.clipboard(clip: ClipData) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
}

/** Small indirection so previews don't launch intents. */
private object Open {
    fun link(context: Context, url: String) {
        runCatching {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
    }
}
