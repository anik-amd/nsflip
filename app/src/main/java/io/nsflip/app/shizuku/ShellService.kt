// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.shizuku

import rikka.sui.Sui

/**
 * Shizuku user service. Runs inside an app_process started by the Shizuku
 * server with the shell (or root) UID, so `pm grant` carries adb privileges.
 *
 * Keep this class free of app-context dependencies: Android may instantiate it
 * without going through the normal app lifecycle.
 */
class ShellService : IShellService.Stub() {

    override fun ping(): Boolean = true

    override fun pmGrant(packageName: String, permission: String): String {
        Sui.init(packageName) // no-op for Shizuku backend, required for Sui
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("pm", "grant", packageName, permission))
            val out = process.inputStream.bufferedReader().use { it.readText() }
            val err = process.errorStream.bufferedReader().use { it.readText() }
            val exit = process.waitFor()
            if (exit == 0) "Done"
            else (err + out).ifBlank { "pm grant failed with exit code $exit" }
        } catch (t: Throwable) {
            "pm grant error: ${t.message ?: t.javaClass.simpleName}"
        }
    }
}
