// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app

import android.app.Application

/**
 * No-op Application. All state is created lazily on first use so that this class
 * stays safe even when Shizuku user services are started via app_process.
 */
class NsFlipApp : Application()
