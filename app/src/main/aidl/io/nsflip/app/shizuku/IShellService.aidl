// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.shizuku;

/**
 * AIDL interface for the Shizuku user service. It runs as the shell UID inside
 * the Shizuku server process, so `pm grant` executes with adb privileges.
 */
interface IShellService {
    boolean ping();

    /** Grants [permission] to [packageName]; returns installer output or error. */
    String pmGrant(String packageName, String permission);
}
