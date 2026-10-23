// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.data

/**
 * Curated Private DNS (DoT) providers, compiled in per plan. Hostnames are
 * verified against provider documentation when this list changes.
 * [DnsEntry.offer] tells the user, in one line, what the provider offers.
 */
object CuratedDns {
    val entries: List<List<DnsEntry>> = listOf(
        listOf(
            DnsEntry("Google", "dns.google", "Fast general resolver, no filtering"),
        ),
        listOf(
            DnsEntry("Cloudflare 1.1.1.1", "one.one.one.one", "Fastest general resolver, privacy-first"),
            DnsEntry("Cloudflare Security", "security.cloudflare-dns.com", "Blocks malware domains"),
            DnsEntry("Cloudflare Family", "family.cloudflare-dns.com", "Blocks malware + adult content"),
        ),
        listOf(
            DnsEntry("AdGuard", "dns.adguard-dns.com", "Blocks ads and trackers in every app"),
            DnsEntry("AdGuard Family", "dns-family.adguard-dns.com", "Ads + malware + adult content"),
        ),
        listOf(
            DnsEntry("Quad9", "dns.quad9.net", "Blocks malware, validates DNSSEC"),
            DnsEntry("Quad9 (no DNSSEC)", "dns11.quad9.net", "Same protection, often faster"),
        ),
        listOf(
            DnsEntry("Mullvad", "dns.mullvad.net", "Privacy-first, no logging"),
            DnsEntry("Mullvad Ad-Block", "adblock.dns.mullvad.net", "Blocks ads and trackers"),
        ),
    )

    val flat: List<DnsEntry> get() = entries.flatten()

    /** Friendly name for a host, from curated list or null. */
    fun nameFor(host: String?): String? =
        host?.let { h -> flat.firstOrNull { it.hostname == h }?.name }
}
