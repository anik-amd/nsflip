// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.data

/**
 * Curated Private DNS (DoT) providers, compiled in per plan. Hostnames are
 * verified against provider documentation when this list changes.
 */
object CuratedDns {
    val entries: List<List<DnsEntry>> = listOf(
        listOf(
            DnsEntry("Google", "dns.google"),
        ),
        listOf(
            DnsEntry("Cloudflare", "one.one.one.one"),
            DnsEntry("Cloudflare Security", "security.cloudflare-dns.com"),
            DnsEntry("Cloudflare Family", "family.cloudflare-dns.com"),
        ),
        listOf(
            DnsEntry("AdGuard Default", "dns.adguard-dns.com"),
            DnsEntry("AdGuard Family", "dns-family.adguard-dns.com"),
        ),
        listOf(
            DnsEntry("Quad9", "dns.quad9.net"),
            DnsEntry("Quad9 (no DNSSEC)", "dns11.quad9.net"),
        ),
        listOf(
            DnsEntry("Mullvad", "dns.mullvad.net"),
            DnsEntry("Mullvad Ad-Block", "adblock.dns.mullvad.net"),
        ),
    )
}
