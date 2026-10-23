# NSFlip

NSFlip makes Android's **Private DNS** one tap away: a home app with curated
DoT providers (Google, Cloudflare, AdGuard, Quad9, Mullvad) plus a Quick
**Settings tile** to toggle instantly. No root, no VPN service, no background
processes — the app just flips a system switch.

## Why a setup step?

Android guards the Private DNS setting with `WRITE_SECURE_SETTINGS`. NSFlip
asks for that permission **once** during setup:

1. **Shizuku (recommended)** — install [Shizuku](https://shizuku.rikka.app/),
   start it, then tap *Grant* in NSFlip. NSFlip runs `pm grant` itself through
   Shizuku's shell privileges. After the grant, Shizuku is never needed again —
   the permission survives reboots and app updates.
2. **Plain adb** — no Shizuku? Connect USB once and run:

   ```sh
   adb shell pm grant io.nsflip.app android.permission.WRITE_SECURE_SETTINGS
   ```

A full reinstall resets the grant (Android policy), so setup reappears.

## Build

Requires JDK 17 and an Android SDK (platform 35). Or use the included
devcontainer (GitHub Codespaces — no local SDK needed) or GitHub Actions.

```sh
gradle :app:assembleDebug :app:testDebugUnitTest
```

Debug APKs are attached to every CI run as artifacts — install + test on device.

## Testing

Unit tests cover hostname validation and the curated list. UI/behavior should
be verified on a **real device** (emulator Private DNS is flaky).

## License

GPLv3 or later. See [LICENSE](LICENSE). Source-file headers use the
SPDX short identifier. Third-party note: the Shizuku API library is
Apache-2.0 (compatible with GPLv3 linking).
