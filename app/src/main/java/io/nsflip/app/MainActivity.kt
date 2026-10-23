// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.nsflip.app.data.CustomEntry
import io.nsflip.app.data.DnsEntry
import io.nsflip.app.data.EntriesRepository
import io.nsflip.app.dns.DnsRepository
import io.nsflip.app.dns.DnsState
import io.nsflip.app.shizuku.GrantManager
import io.nsflip.app.ui.CustomEntryInput
import io.nsflip.app.ui.HomeScreen
import io.nsflip.app.ui.NSFlipTheme
import io.nsflip.app.ui.SetupScreen
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

@Composable
private fun AppBody(refreshTick: State<Int>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var granted by remember { mutableStateOf(GrantManager.isGranted(context)) }
    var grantStatus by remember { mutableStateOf(GrantManager.status(context)) }
    var error by remember { mutableStateOf<String?>(null) }
    var dnsState by remember { mutableStateOf(DnsState(DnsState.MODE_AUTO, null)) }

    val customFlow = remember(context) { EntriesRepository.customEntries(context) }
    val custom by customFlow.collectAsState(initial = emptyList())

    fun refresh() {
        granted = GrantManager.isGranted(context)
        grantStatus = GrantManager.status(context)
        dnsState = DnsRepository.currentState(context)
        error = null
    }

    // Refresh on tick, on composition, and every resume from background.
    LaunchedEffect(refreshTick.value) { refresh() }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) scope.launch { refresh() }
            }
        )
    }

    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        )
        return
    }

    if (granted) {
        val connect: (DnsEntry) -> Unit = { entry ->
            val ok = runCatching { DnsRepository.connect(context, entry) }.getOrDefault(false)
            if (ok) {
                scope.launch { EntriesRepository.setLastUsed(context, entry.hostname) }
            } else {
                error = "Write failed — permission lost. Re-run setup."
            }
            scope.launch { refresh() }
        }
        HomeScreen(
            state = dnsState,
            custom = custom,
            onConnect = connect,
            onTurnOff = {
                val ok = runCatching { DnsRepository.turnOff(context) }.getOrDefault(false)
                if (!ok) error = "Write failed — permission lost. Re-run setup."
                scope.launch { refresh() }
            },
            onAdd = { input: CustomEntryInput ->
                scope.launch {
                    EntriesRepository.addCustom(context, CustomEntry(input.name, input.hostname))
                    EntriesRepository.setLastUsed(context, input.hostname)
                    refresh()
                }
            },
            onDeleteCustom = { hostname ->
                scope.launch {
                    EntriesRepository.deleteCustom(context, hostname)
                    if (dnsState.activeHost == hostname) {
                        runCatching { DnsRepository.turnOff(context) }
                    }
                    refresh()
                }
            },
        )
    } else {
        SetupScreen(
            status = grantStatus,
            onRefresh = { scope.launch { refresh() } },
            onError = { error = it },
        )
    }
}
