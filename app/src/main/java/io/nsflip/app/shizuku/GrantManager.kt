// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicReference

/**
 * Owns the Shizuku bootstrap: binder state, permission prompt and the one-tap
 * `pm grant WRITE_SECURE_SETTINGS` through a Shizuku user service.
 *
 * After the grant, Shizuku is not used anywhere else in the app —
 * the WRITE_SECURE_SETTINGS permission persists by itself.
 */
object GrantManager {

    const val PERMISSION = "android.permission.WRITE_SECURE_SETTINGS"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 2001

    /** UI-facing status snapshot. */
    data class Status(
        val granted: Boolean,
        val shizukuInstalled: Boolean,
        val shizukuRunning: Boolean,
        val shizukuPermission: Boolean,
    )

    fun isGranted(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun status(context: Context): Status {
        val installed = runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        }.getOrNull() != null
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val perm = runCatching {
            running && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        return Status(
            granted = isGranted(context),
            shizukuInstalled = installed,
            shizukuRunning = running,
            shizukuPermission = perm,
        )
    }

    /** Shows the Shizuku permission prompt. Result arrives via [listener]. */
    fun requestShizukuPermission(listener: (granted: Boolean) -> Unit) {
        val reply = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == REQUEST_CODE) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    listener(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(reply)
        Shizuku.requestPermission(REQUEST_CODE)
    }

    /**
     * Starts the user service and runs `pm grant <self> WRITE_SECURE_SETTINGS`.
     * Returns null on success, an error message otherwise.
     */
    suspend fun grantSelf(context: Context): String? =
        withContext(Dispatchers.IO) {
            val args = Shizuku.UserServiceArgs(ComponentName(context, ShellService::class.java))
                .processNameSuffix("pmservice")
                .version(1)
                .daemon(false)

            val svcRef = AtomicReference<IShellService?>(null)

            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    svcRef.set(IShellService.Stub.asInterface(binder))
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    svcRef.set(null)
                }
            }

            try {
                Shizuku.bindUserService(args, conn)
                val deadline = System.currentTimeMillis() + 15_000
                while (System.currentTimeMillis() < deadline) {
                    val s = svcRef.get()
                    if (s != null) {
                        val out = s.pmGrant(context.packageName, PERMISSION)
                        Shizuku.unbindUserService(args, null, false)
                        return@withContext if (out.isNullOrBlank() ||
                            out.contains("granted", ignoreCase = true)
                        ) null else out
                    }
                    delay(250)
                }
                "Timed out starting Shizuku service"
            } catch (t: Throwable) {
                "Shizuku error: ${t.message ?: t.javaClass.simpleName}"
            }
        }
}
