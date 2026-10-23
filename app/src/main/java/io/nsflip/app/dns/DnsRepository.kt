// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.dns

import android.content.Context
import android.provider.Settings
import io.nsflip.app.data.DnsEntry

/** Current Private DNS state as seen by the UI and the tile. */
data class DnsState(
    val mode: String,          // Settings.Global value: "off" | "auto" | "hostname" | unknown
    val specifier: String?,    // active DoT host when mode == "hostname"
) {
    val isActiveHost: Boolean get() = mode == MODE_HOSTNAME && !specifier.isNullOrBlank()
    val activeHost: String? get() = if (isActiveHost) specifier else null

    companion object {
        // Settings.Global column names are public constants, but the SDK's
        // Settings.Global fields for them are @hide — use the literal column names.
        const val KEY_PRIVATE_DNS_MODE = "private_dns_mode"
        const val KEY_PRIVATE_DNS_SPECIFIER = "private_dns_specifier"

        const val MODE_OFF = "off"
        const val MODE_AUTO = "auto"
        const val MODE_HOSTNAME = "hostname"
    }
}

/** Reads/writes the system Private DNS settings. Needs WRITE_SECURE_SETTINGS to write. */
object DnsRepository {

    fun currentState(context: Context): DnsState = DnsState(
        mode = readMode(context),
        specifier = readSpecifier(context),
    )

    private fun readMode(context: Context): String =
        Settings.Global.getString(context.contentResolver, DnsState.KEY_PRIVATE_DNS_MODE)
            ?: DnsState.MODE_AUTO // devices default to auto when unset

    private fun readSpecifier(context: Context): String? =
        Settings.Global.getString(context.contentResolver, DnsState.KEY_PRIVATE_DNS_SPECIFIER)

    private fun write(context: Context, mode: String?, specifier: String?): Boolean =
        runCatching {
            if (mode != null) {
                Settings.Global.putString(
                    context.contentResolver,
                    DnsState.KEY_PRIVATE_DNS_MODE,
                    mode
                )
            }
            if (specifier != null || mode == DnsState.MODE_HOSTNAME) {
                Settings.Global.putString(
                    context.contentResolver,
                    DnsState.KEY_PRIVATE_DNS_SPECIFIER,
                    specifier
                )
            }
        }.isSuccess

    /** Connect to a DoT host: writes mode=hostname + specifier, persists last-used. */
    fun connect(context: Context, entry: DnsEntry): Boolean {
        val ok = write(context, DnsState.MODE_HOSTNAME, entry.hostname)
        return ok
    }

    /** Turn Private DNS off (mode=off, clears specifier). */
    fun turnOff(context: Context): Boolean = write(context, DnsState.MODE_OFF, null)
}
