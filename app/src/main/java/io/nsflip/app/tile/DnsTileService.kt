// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.nsflip.app.MainActivity
import io.nsflip.app.R
import io.nsflip.app.data.CuratedDns
import io.nsflip.app.data.DnsEntry
import io.nsflip.app.data.EntriesRepository
import io.nsflip.app.dns.DnsRepository
import io.nsflip.app.dns.DnsState
import io.nsflip.app.shizuku.GrantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Quick Settings tile.
 *  - Active: mode=hostname with a specifier; subtitle shows the host.
 *  - Inactive: off/auto/unknown or not set up.
 *  - Tap: toggle last-used host on/off. Long-press: open the app (system-provided).
 */
class DnsTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    private fun update() {
        val tile = qsTile ?: return

        if (!GrantManager.isGranted(this)) {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.tile_not_set_up)
            tile.updateTile()
            return
        }

        val state = DnsRepository.currentState(this)
        val activeHost = state.activeHost
        if (activeHost != null) {
            tile.state = Tile.STATE_ACTIVE
            tile.subtitle = activeHost
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = if (state.mode == DnsState.MODE_AUTO) {
                getString(R.string.status_auto_short)
            } else {
                getString(R.string.tile_off)
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!GrantManager.isGranted(this)) {
            openApp()
            update()
            return
        }
        val state = DnsRepository.currentState(this)
        scope.launch {
            val activeHost = state.activeHost
            if (activeHost != null) {
                DnsRepository.turnOff(applicationContext)
            } else {
                // Re-apply last-used host; fall back to the first curated host.
                val last = EntriesRepository.lastUsedOnce(applicationContext)
                val target = last?.let { DnsEntry(name = it, hostname = it) }
                    ?: CuratedDns.entries.first().first()
                DnsRepository.connect(applicationContext, target)
                EntriesRepository.setLastUsed(applicationContext, target.hostname)
            }
            runCatching { update() } // qsTile may be null when the shade was closed
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val launch = runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                super.startActivityAndCollapse(
                    PendingIntent.getActivity(
                        this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                super.startActivityAndCollapse(intent)
            }
        }
        launch.recoverCatching { openAppFallback(intent) }
    }

    private fun openAppFallback(intent: Intent) {
        @Suppress("DEPRECATION")
        super.startActivityAndCollapse(intent)
    }
}
