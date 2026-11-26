package com.astra.vpn.data

/**
 * A single public relay published by the VPN Gate academic project
 * (University of Tsukuba). Servers are run by volunteers — fine for
 * everyday HTTPS traffic (YouTube, social apps, AI tools), but never
 * for anything that isn't already end-to-end encrypted.
 */
data class VpnServer(
    val hostName: String,
    val ip: String,
    val countryLong: String,
    val countryShort: String,
    val pingMs: Int,
    val speedBps: Long,
    val numSessions: Int,
    val uptimeMs: Long,
    val openVpnConfigBase64: String
) {
    val speedMbps: Double get() = speedBps / 1_000_000.0

    /** ("tcp"|"udp", port) read from the embedded .ovpn, or null if unreadable. */
    val transport: Pair<String, Int>? by lazy {
        try {
            val text = String(java.util.Base64.getMimeDecoder().decode(openVpnConfigBase64))
            val proto = Regex("(?m)^proto\\s+(\\w+)").find(text)?.groupValues?.get(1)?.lowercase()
            val port = Regex("(?m)^remote\\s+\\S+\\s+(\\d+)").find(text)?.groupValues?.get(1)?.toInt()
            if (proto != null && port != null) proto.take(3) to port else null
        } catch (e: Exception) { null }
    }

    /** Lower is better: TCP/443 looks like HTTPS and survives filtering best. */
    val filterResistanceRank: Int get() = when {
        transport?.first == "tcp" && transport?.second == 443 -> 0
        transport?.first == "tcp" -> 1
        else -> 2
    }
    val flagEmoji: String get() = countryCodeToFlag(countryShort)
}

private fun countryCodeToFlag(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val base = 0x1F1E6
    val first = countryCode[0].uppercaseChar().code - 'A'.code + base
    val second = countryCode[1].uppercaseChar().code - 'A'.code + base
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
