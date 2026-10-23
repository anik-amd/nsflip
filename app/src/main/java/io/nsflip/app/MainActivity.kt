// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.nsflip.app.data.CuratedDns
import io.nsflip.app.data.CustomEntry
import io.nsflip.app.data.DnsEntry
import io.nsflip.app.data.EntriesRepository
import io.nsflip.app.dns.DnsRepository
import io.nsflip.app.dns.DnsState
import io.nsflip.app.shizuku.GrantManager
import io.nsflip.app.ui.HomeScreen
import io.nsflip.app.ui.NSFlipTheme
import io.nsflip.app.ui.OnboardingScreen
import io.nsflip.app.ui.ProvidersScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val refreshTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NSFlipTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppBody(refreshTick)
                }
            }
        }
    }

    fun requestRefresh() {
        refreshTick.intValue = refreshTick.intValue + 1
    }

    override fun onStart() {
        super.onStart()
        requestRefresh()
    }
}

private enum class Screen { Home, Providers }

@Composable
private fun AppBody(refreshTick: State<Int>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var granted by remember { mutableStateOf(GrantManager.isGranted(context)) }
    var grantStatus by remember { mutableStateOf(GrantManager.status(context)) }
    var grantError by remember { mutableStateOf<String?>(null) }
    var dnsState by remember { mutableStateOf(DnsState(DnsState.MODE_AUTO, null)) }
    var setupSkipped by rememberSaveable { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    val customFlow = remember(context) { EntriesRepository.customEntries(context) }
    val custom by customFlow.collectAsState(initial = emptyList())
    val lastUsedFlow = remember(context) { EntriesRepository.lastUsed(context) }
    val lastUsedHost by lastUsedFlow.collectAsState(initial = null as String?)

    fun refresh() {
        granted = GrantManager.isGranted(context)
        grantStatus = GrantManager.status(context)
        dnsState = DnsRepository.currentState(context)
    }

    LaunchedEffect(refreshTick.value) { refresh() }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) scope.launch { refresh() }
            }
        )
    }

    BackHandler(enabled = screen == Screen.Providers) { screen = Screen.Home }

    fun connect(entry: DnsEntry, then: () -> Unit = {}) {
        val ok = runCatching { DnsRepository.connect(context, entry) }.getOrDefault(false)
        if (ok) {
            scope.launch { EntriesRepository.setLastUsed(context, entry.hostname) }
        } else {
            // Permission lost (uninstall/reinstall or revoked): back to onboarding.
            granted = false
            setupSkipped = false
            grantError = "Write failed — the permission is gone. Re-run setup."
        }
        refresh()
        then()
    }

    val activeHost = dnsState.activeHost
    val lastHost = lastUsedHost
    val providerName = when {
        activeHost != null -> displayName(activeHost, custom)
        lastHost != null -> displayName(lastHost, custom)
        else -> "No provider selected"
    }

    if (!granted && !setupSkipped) {
        OnboardingScreen(
            status = grantStatus,
            grantError = grantError,
            onRefresh = { refresh() },
            onAllowShizuku = {
                GrantManager.requestShizukuPermission { refresh() }
            },
            onGrantOneTap = { onSuccess ->
                scope.launch {
                    val err = GrantManager.grantSelf(context)
                    if (err == null) {
                        refresh()
                        onSuccess()
                    } else {
                        grantError = err
                    }
                }
            },
            onDone = { refresh() },
            onSkip = { setupSkipped = true },
        )
        return
    }

    if (screen == Screen.Providers) {
        ProvidersScreen(
            state = dnsState,
            custom = custom,
            onConnect = { entry -> connect(entry) { screen = Screen.Home } },
            onAdd = { entry: CustomEntry ->
                scope.launch {
                    EntriesRepository.addCustom(context, entry)
                    EntriesRepository.setLastUsed(context, entry.hostname)
                }
            },
            onRename = { host, newName ->
                scope.launch { EntriesRepository.updateCustom(context, host, newName) }
            },
            onDelete = { host ->
                scope.launch {
                    EntriesRepository.deleteCustom(context, host)
                    if (dnsState.activeHost == host) {
                        runCatching { DnsRepository.turnOff(context) }
                    }
                    refresh()
                }
            },
            onBack = { screen = Screen.Home },
        )
    } else {
        HomeScreen(
            state = dnsState,
            providerName = providerName,
            onToggle = { on ->
                if (on) {
                    val host = activeHost ?: lastHost
                    if (host != null) {
                        connect(DnsEntry(displayName(host, custom), host))
                    } else {
                        screen = Screen.Providers
                    }
                } else {
                    runCatching { DnsRepository.turnOff(context) }
                    refresh()
                }
            },
            onOpenProviders = { screen = Screen.Providers },
        )
    }
}

private fun displayName(host: String, custom: List<CustomEntry>): String =
    CuratedDns.nameFor(host)
        ?: custom.firstOrNull { it.hostname == host }?.name
        ?: host
