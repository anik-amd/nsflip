// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.nsflip.app.dns.DnsState

/**
 * Home: big centered connect switch, live status, selected provider,
 * and a link to the provider selection page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: DnsState,
    providerName: String,
    onToggle: (Boolean) -> Unit,
    onOpenProviders: () -> Unit,
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(title = { Text("NSFlip") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val on = state.activeHost != null

            BigConnectSwitch(checked = on, onToggle = onToggle)

            Spacer(Modifier.height(28.dp))
            StatusLine(on = on)

            Spacer(Modifier.height(10.dp))
            Text(
                providerName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (on) state.activeHost ?: "" else "Tap the switch to connect",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onOpenProviders) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                Text("  Change provider")
            }
        }
    }
}

@Composable
private fun BigConnectSwitch(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onToggle,
        modifier = Modifier.scale(2.4f),
        thumbContent = {
            Icon(
                if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = if (checked) "Connected" else "Off",
            )
        },
    )
}

@Composable
private fun StatusLine(on: Boolean) {
    val color by animateColorAsState(
        targetValue = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "statusColor",
    )
    Text(
        if (on) "Private DNS is on" else "Private DNS is off",
        style = MaterialTheme.typography.titleLarge,
        color = color,
    )
}
