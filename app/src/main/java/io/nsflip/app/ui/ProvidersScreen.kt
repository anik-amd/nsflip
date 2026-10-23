// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import io.nsflip.app.data.CustomEntry
import io.nsflip.app.data.DnsEntry
import io.nsflip.app.dns.DnsState

/**
 * Provider selection: what each provider offers, tap to connect,
 * gear opens an action sheet; plus adds a custom host.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    state: DnsState,
    custom: List<CustomEntry>,
    onConnect: (DnsEntry) -> Unit,
    onAdd: (CustomEntry) -> Unit,
    onRename: (hostname: String, newName: String) -> Unit,
    onDelete: (hostname: String) -> Unit,
    onBack: () -> Unit,
) {
    var sheetHost by rememberSaveable { mutableStateOf<String?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var editHost by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteHost by rememberSaveable { mutableStateOf<String?>(null) }

    val sheetEntry: DnsEntry? = sheetHost?.let { host ->
        CuratedDns.flat.firstOrNull { it.hostname == host }
            ?: custom.firstOrNull { it.hostname == host }?.let { DnsEntry(it.name, it.hostname) }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Provider") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add custom provider")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item { SectionLabel("Curated") }
            items(CuratedDns.flat, key = { "c-" + it.hostname }) { entry ->
                ProviderRow(
                    entry = entry,
                    active = state.activeHost == entry.hostname,
                    onConnect = { onConnect(entry) },
                    onOpenActions = { sheetHost = entry.hostname },
                )
            }
            item { SectionLabel("Your hosts") }
            if (custom.isEmpty()) {
                item {
                    Text(
                        "None yet — add one with the + button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(custom, key = { "u-" + it.hostname }) { entry ->
                    val dnsEntry = DnsEntry(entry.name, entry.hostname, offer = "Custom host")
                    ProviderRow(
                        entry = dnsEntry,
                        active = state.activeHost == entry.hostname,
                        onConnect = { onConnect(dnsEntry) },
                        onOpenActions = { sheetHost = entry.hostname },
                    )
                }
            }
        }
    }

    // Action sheet for one provider (modern M3 bottom sheet)
    if (sheetEntry != null) {
        val host = sheetEntry!!.hostname
        val isCustom = custom.any { it.hostname == host }
        ModalBottomSheet(onDismissRequest = { sheetHost = null }) {
            Text(
                sheetEntry!!.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                host,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text(if (state.activeHost == host) "Reconnect" else "Connect") },
                leadingContent = { Icon(Icons.Filled.Link, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConnect(sheetEntry!!); sheetHost = null },
            )
            if (isCustom) {
                ListItem(
                    headlineContent = { Text("Edit name") },
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editHost = host; sheetHost = null },
                )
                ListItem(
                    headlineContent = {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteHost = host; sheetHost = null },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // Add custom provider dialog
    if (showAdd) {
        ProviderEditDialog(
            title = "Add host",
            initialName = "",
            initialHost = "",
            hostEditable = true,
            onDismiss = { showAdd = false },
            onSave = { name, host ->
                onAdd(CustomEntry(name, host))
                showAdd = false
            },
        )
    }

    // Rename custom provider dialog
    editHost?.let { host ->
        val current = custom.firstOrNull { it.hostname == host }
        if (current != null) {
            ProviderEditDialog(
                title = "Edit name",
                initialName = current.name,
                initialHost = host,
                hostEditable = false,
                onDismiss = { editHost = null },
                onSave = { name, _ ->
                    onRename(host, name)
                    editHost = null
                },
            )
        }
    }

    // Delete confirmation
    deleteHost?.let { host ->
        val current = custom.firstOrNull { it.hostname == host }
        AlertDialog(
            onDismissRequest = { deleteHost = null },
            title = { Text("Delete host?") },
            text = { Text("“${current?.name ?: host}” will be removed from your hosts.") },
            confirmButton = {
                TextButton(onClick = { onDelete(host); deleteHost = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteHost = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ProviderRow(
    entry: DnsEntry,
    active: Boolean,
    onConnect: () -> Unit,
    onOpenActions: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                if (active) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        },
        supportingContent = {
            Column {
                Text(
                    entry.hostname,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.offer?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onOpenActions) {
                Icon(Icons.Outlined.Settings, contentDescription = "Options for ${entry.name}")
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (active)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onConnect),
    )
}

/** Modern M3 dialog used for both add and rename. */
@Composable
private fun ProviderEditDialog(
    title: String,
    initialName: String,
    initialHost: String,
    hostEditable: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, host: String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var host by rememberSaveable(initialHost) { mutableStateOf(initialHost) }

    val hostError = if (hostEditable) DnsEntry.validateHostname(host) else null
    val nameError = if (name.isBlank()) "Enter a name" else null
    val valid = hostError == null && nameError == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                )
                if (hostEditable) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("DoT hostname") },
                        placeholder = { Text("e.g. dns.adguard-dns.com") },
                        singleLine = true,
                        isError = hostError != null,
                        supportingText = { hostError?.let { Text(it) } },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(name.trim(), host.trim().lowercase()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
