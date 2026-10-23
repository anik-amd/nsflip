# NSFlip — Android Private DNS Switch App — Build Plan

## Project
- **Name:** NSFlip
- **Type:** Open source, **GPLv3** (GNU General Public License v3.0)
  - Repo must include `LICENSE` (GPLv3 full text), GPLv3 headers on source files optional but recommended, `README.md` noting license + how to grant WRITE_SECURE_SETTINGS safely
  - No non-OS dependencies that would violate GPL (Shizuku API is Apache-2.0 — compatible)
- **Goal:** Android app to switch the device's **Private DNS** (encrypted DNS) quickly, mainly for ad/malware blocking. Android's Private DNS setting is buried; NSFlip provides a home screen picker plus a **Quick Settings tile** to toggle instantly.

## Core design decisions (fixed — do not change)
- **Approach:** Android's native **Private DNS** (`Settings.Global.PRIVATE_DNS_MODE` / `PRIVATE_DNS_SPECIFIER`), no VPN service, no root.
- **Privilege:** App needs `WRITE_SECURE_SETTINGS`. Granted **once at setup** via **Shizuku** by running `pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS`. After grant, Shizuku never needed again — grant persists across reboots and app updates (lost only on uninstall/reinstall; setup reappears, 2 taps or adb fallback).
- **Tile:** Tap = apply last-used DNS (toggle off if that host already active). Long-press = deep link into app home page. Custom popup on long-press impossible (Android reserves it) — accepted.
- **DoT hostnames only** — curated + custom entries are hostnames (`dns.adguard-dns.com`), never raw IPs.

## Stack
- Kotlin, Jetpack Compose, Material 3, single-module app
- minSdk 29 (Android 10)
- Shizuku API (shizuku-api, Apache-2.0) only for the setup grant; handle in R8/ProGuard
- DataStore (Preferences) for persistence
- Permissions: `WRITE_SECURE_SETTINGS` (declared in manifest, granted via Shizuku) + `POST_NOTIFICATIONS` if needed for tile prompts on 13+

## Architecture
- `GrantManager` — permission check + Shizuku grant flow + plain-adb fallback text
- `DnsRepository` — read/write `PRIVATE_DNS_MODE` (`off`/`hostname`) & `PRIVATE_DNS_SPECIFIER`; treat mode `auto` as disconnected for UI/tile
- `DnsEntriesDataSource` (DataStore) — custom entries (name + hostname), last-used host; curated list = compiled-in constants
- `DnsTileService` — TileService: active/inactive + subtitle (host), tap applies/removes last-used host
- `MainActivity` — Compose UI only; no background services, no persistent notification, no network traffic (app flips a system switch only)

## Screens
1. **Setup (first run / ungranted):** checklist — Shizuku installed → Shizuku permission → auto run `pm grant` → granted ✓. Fallback: show the plain adb command copy-paste. Include "refresh state" re-check button.
2. **Home:**
   - Status card: current mode + active host, or "Off"
   - Curated list (tap to connect, selected shows live status): Google `dns.google`, Cloudflare `one.one.one.one` / `security.cloudflare-dns.com` / `family.cloudflare-dns.com`, AdGuard `dns.adguard-dns.com` / `dns-family.adguard-dns.com`, Quad9 `dns.quad9.net` / `dns11.quad9.net`, Mullvad `dns.mullvad.net` / `adblock.dns.mullvad.net` — **verify each exact DoT hostname against provider docs during implementation**
   - Custom list: tap to connect, per-item delete (swipe or icon)
   - Add (+) dialog: friendly name + hostname (regex-validate, hint "hostname only, e.g. dns.adguard-dns.com")
   - Connecting writes mode + specifier immediately, stores last-used
3. **Tile (`DNS`):** active when specifier set (subtitle = host), inactive on `off`/`auto`; tap = connect last-used / toggle off; long-press → app home

## Build order
1. Scaffold: Compose app, manifest, Material 3, GPLv3 `LICENSE` + README stub
2. `GrantManager` + setup screen (Shizuku + adb fallback)
3. `DnsRepository` + curated list + connect + status card
4. Custom entries: DataStore, add dialog with validation, delete, connect
5. `DnsTileService`: last-used, tile state, tap toggle, long-press deep link, subtitle
6. Polish: icons, empty/error states, dialogs, test on real device

## Caveats / acceptance criteria
- Test on **real device** preferred (emulator Private DNS flaky)
- Degrade gracefully: tile shows "not set up" state when ungranted; no crash if Shizuku missing
- App never runs in background; no Shizuku calls after grant
- GPLv3 enforced: LICENSE file present, source headers noted in README
- Full reinstall = grant lost → setup reappears (expected)

## Naming verification (done)
- `NSFlip` — no Play Store match, unique ✓
- Checked and rejected as taken: Switchboard, Sieve, Reroute, Nameplate, Handoff, Dialtone, Quick DNS
