// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.data

/** A Private DNS provider entry: friendly name + DoT hostname (never a raw IP). */
data class DnsEntry(
    val name: String,
    val hostname: String,
) {
    override fun toString(): String = "$name ($hostname)"

    companion object {
        /**
         * Valid DoT hostname: at least one label, alnum start/end per label,
         * lowercase letters/digits/hyphens/dots only. No scheme, port, path or IP.
         */
        private val HOSTNAME_REGEX =
            Regex("""^(?=.{1,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$""")

        private val IP_REGEX =
            Regex("""^\d{1,3}(\.\d{1,3}){3}$|^\[[0-9a-fA-F:]+]$""")

        fun validateHostname(raw: String): String? {
            val host = raw.trim().lowercase()
            if (host.isEmpty()) return "Enter a hostname"
            if (host.length > 253) return "Too long"
            if (IP_REGEX.matches(host)) return "Enter a DoT hostname, not an IP"
            if (!HOSTNAME_REGEX.matches(host)) return "Invalid hostname"
            if (host.count { it == '.' } < 1) return "Needs a domain, e.g. dns.adguard-dns.com"
            return null
        }
    }
}
