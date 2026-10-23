// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.nsflip.app.shizuku.GrantManager
import kotlinx.coroutines.launch

private const val SHIZUKU_URL = "https://shizuku.rikka.app/"
private const val ADB_COMMAND =
    "adb shell pm grant io.nsflip.app ${GrantManager.PERMISSION}"

/**
 * Paged onboarding: what the app does → install Shizuku → start Shizuku → grant.
 * One step per page, each with its own action and live status.
 */
@Composable
fun OnboardingScreen(
    status: GrantManager.Status,
    grantError: String?,
    onRefresh: () -> Unit,
    onAllowShizuku: () -> Unit,
    onGrantOneTap: (onSuccess: () -> Unit) -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 4 }

    // Auto-advance (forward only) to the first unfinished step as status changes.
    LaunchedEffect(status.shizukuInstalled, status.shizukuRunning, status.granted) {
        val target = when {
            !status.shizukuInstalled -> 1
            !status.shizukuRunning -> 2
            !status.granted -> 3
            else -> 0
        }
        if (target > pagerState.currentPage && pagerState.currentPage != 0) {
            pagerState.scrollToPage(target)
        } else if (target > 0 && pagerState.currentPage == 0 && target > 1 && !status.granted) {
            // Reinstall case: Shizuku already there, land on the next real step.
            pagerState.scrollToPage(target)
        }
    }
    // Re-check whenever a page is settled.
    LaunchedEffect(pagerState.currentPage) { onRefresh() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSkip) { Text("Skip setup") }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> InstallPage(status)
                    2 -> StartPage(status, onRefresh)
                    else -> GrantPage(status, grantError, onAllowShizuku, onGrantOneTap, onDone, onRefresh)
                }
            }

            PageDots(pagerState.currentPage, pageCount = 4)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) { Text("Back") }
                } else {
                    Spacer(Modifier.size(1.dp))
                }
                Button(onClick = {
                    scope.launch {
                        if (pagerState.currentPage < 3) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }) { Text(if (pagerState.currentPage == 0) "Get started" else "Next") }
            }
        }
    }
}

@Composable
private fun PageDots(current: Int, pageCount: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (active) 22.dp else 8.dp, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
            )
        }
    }
}

@Composable
private fun PageScaffold(
    title: String,
    body: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun StatusChip(ok: Boolean, doneText: String, todoText: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
        )
        Text(
            "  " + if (ok) doneText else todoText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (ok) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WelcomePage() {
    PageScaffold(
        title = "Private DNS, one tap",
        body = "NSFlip switches Android's built-in Private DNS — encrypted DNS that can block ads and malware for every app on this phone. No root, no VPN, nothing running in the background.",
    ) {
        Box(
            modifier = Modifier
                .size(width = 220.dp, height = 100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DNS", style = MaterialTheme.typography.titleLarge)
                Text(
                    "dns.adguard-dns.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "One-time setup needs a permission. Two minutes, then you never see this again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun InstallPage(status: GrantManager.Status) {
    val context = LocalContext.current
    PageScaffold(
        title = "Install Shizuku",
        body = "Shizuku is a free, open-source helper that lets NSFlip grant its own permission once — no PC needed afterwards. Get it from the Play Store or GitHub.",
    ) {
        StatusChip(status.shizukuInstalled, "Shizuku installed", "Shizuku not installed yet")
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_URL))) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Get Shizuku") }
    }
}

@Composable
private fun StartPage(status: GrantManager.Status, onRefresh: () -> Unit) {
    val context = LocalContext.current
    PageScaffold(
        title = "Start Shizuku",
        body = "Open Shizuku once and start it (wireless debugging on the same device, or USB once). Come back when it says “running”.",
    ) {
        StatusChip(status.shizukuRunning, "Shizuku is running", "Shizuku is not running")
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Open Shizuku") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Re-check after starting")
        }
    }
}

@Composable
private fun GrantPage(
    status: GrantManager.Status,
    grantError: String?,
    onAllowShizuku: () -> Unit,
    onGrantOneTap: (onSuccess: () -> Unit) -> Unit,
    onDone: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    PageScaffold(
        title = "Grant the permission",
        body = "Last step: NSFlip runs a single pm grant through Shizuku. After this, Shizuku is never needed again — the permission survives reboots and updates.",
    ) {
        StatusChip(status.granted, "Permission granted — you're done", "Permission not granted yet")
        Spacer(Modifier.height(20.dp))

        when {
            status.granted -> {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Start using NSFlip")
                }
            }
            status.shizukuRunning && status.shizukuPermission -> {
                Button(
                    onClick = { onGrantOneTap { onDone() } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Grant one-tap") }
            }
            status.shizukuRunning -> {
                Button(onClick = onAllowShizuku, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow Shizuku access")
                }
            }
        }

        grantError?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "No Shizuku? One-time adb from a PC works too:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ADB_COMMAND,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("nsflip", ADB_COMMAND))
            }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy adb command") }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRefresh) { Text("Re-check state") }
        }
    }
}
