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
    val flagEmoji: String get() = countryCodeToFlag(countryShort)
}

private fun countryCodeToFlag(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val base = 0x1F1E6
    val first = countryCode[0].uppercaseChar().code - 'A'.code + base
    val second = countryCode[1].uppercaseChar().code - 'A'.code + base
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
