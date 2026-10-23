// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.nsflip.app.data.CuratedDns
import io.nsflip.app.data.DnsEntry
import io.nsflip.app.dns.DnsState

/**
 * Home screen: live status card, curated providers, custom hosts.
 * Grid adapts columns to width; section rows span full width (adaptive skill).
 */

private data class HomeModels(
    val state: DnsState,
)

@Composable
fun HomeScreen(
    state: DnsState,
    custom: List<io.nsflip.app.data.CustomEntry>,
    onConnect: (DnsEntry) -> Unit,
    onTurnOff: () -> Unit,
    onAdd: (CustomEntryInput) -> Unit,
    onDeleteCustom: (String) -> Unit,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add custom host")
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 88.dp, top = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StatusCard(state, onTurnOff)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Curated providers", "tap to connect")
            }

            CuratedDns.entries.forEach { provider ->
                items(provider, key = { it.hostname }) { entry ->
                    DnsCard(
                        name = entry.name,
                        subtitle = entry.hostname,
                        selected = state.activeHost == entry.hostname,
                        onClick = { onConnect(entry) },
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Your hosts", "tap to connect, delete with the trash icon")
            }

            if (custom.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No custom hosts yet. Use the + button to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(custom, key = { it.hostname }) { entry ->
                    DnsCard(
                        name = entry.name,
                        subtitle = entry.hostname,
                        selected = state.activeHost == entry.hostname,
                        onClick = { onConnect(DnsEntry(entry.name, entry.hostname)) },
                        trailing = {
                            IconButton(onClick = { onDeleteCustom(entry.hostname) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${entry.name}")
                            }
                        },
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Changes apply instantly; let Android pick a resolver slot. If a host fails, Private DNS falls back silently — check the app network note.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    if (showAddDialog) {
        AddEntryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = {
                onAdd(it)
                showAddDialog = false
            },
        )
    }
}

data class CustomEntryInput(val name: String, val hostname: String)

@Composable
private fun StatusCard(state: DnsState, onTurnOff: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (state.activeHost != null)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.activeHost != null) "Private DNS on" else "Private DNS off",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    state.activeHost ?: "System default / automatic",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.activeHost != null) {
                FilledTonalButton(onClick = onTurnOff) {
                    Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                    Text("  Turn off")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, hint: String) {
    Column(Modifier.padding(top = 10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DnsCard(
    name: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    if (selected) {
                        Text(
                            "  ✔ connected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun AddEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (CustomEntryInput) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }

    val hostError = DnsEntry.validateHostname(host)
    val nameError = if (name.isBlank()) "Enter a name" else null
    val valid = hostError == null && nameError == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add host") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("DoT hostname") },
                    placeholder = { Text("e.g. dns.adguard-dns.com") },
                    singleLine = true,
                    isError = hostError != null,
                    supportingText = { hostError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onAdd(CustomEntryInput(name.trim(), host.trim().lowercase())) },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
