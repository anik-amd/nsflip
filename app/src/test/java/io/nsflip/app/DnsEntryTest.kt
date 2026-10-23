// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app

import io.nsflip.app.data.CuratedDns
import io.nsflip.app.data.DnsEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsEntryTest {

    private fun valid(host: String) = assertEquals(null, DnsEntry.validateHostname(host))
    private fun invalid(host: String) = assertTrue(
        "expected invalid: $host",
        DnsEntry.validateHostname(host) != null,
    )

    @Test
    fun `accepts common DoT hostnames`() {
        valid("dns.google")
        valid("one.one.one.one")
        valid("dns.adguard-dns.com")
        valid("dns-family.adguard-dns.com")
        valid("adblock.dns.mullvad.net")
        valid("a.co")
    }

    @Test
    fun `rejects empty and raw IPs`() {
        invalid("")
        invalid("   ")
        invalid("1.1.1.1")
        invalid("2606:4700:4700::1111")
        invalid("[2606:4700:4700::1111]")
    }

    @Test
    fun `rejects urls paths and bad labels`() {
        invalid("https://dns.google")
        invalid("dns.google:853/ads")
        invalid("-.example.com")
        invalid("example-.com")
        invalid("a..b.com")
        invalid(".com")
        invalid("com.")
    }

    @Test
    fun `rejects labels over 63 chars`() {
        val long = "a".repeat(64)
        invalid("$long.example.com")
        valid("${"a".repeat(63)}.example.com")
    }

    @Test
    fun `curated list is all valid hostnames`() {
        val all = CuratedDns.entries.flatten()
        assertTrue(all.size >= 10)
        all.forEach { assertEquals("curated entry: $it", null, DnsEntry.validateHostname(it.hostname)) }
    }

    @Test
    fun `entry toString readable`() {
        assertEquals("Google (dns.google)", DnsEntry("Google", "dns.google").toString())
        assertFalse(DnsEntry("x", "y").toString().isBlank())
    }
}
