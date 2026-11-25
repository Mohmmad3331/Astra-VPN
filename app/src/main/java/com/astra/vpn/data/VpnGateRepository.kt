package com.astra.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Fetches the public server list from the VPN Gate academic project's
 * official CSV API (http://www.vpngate.net/api/iphone/). This is a
 * publicly documented, open endpoint intended for exactly this kind
 * of client — no account, no scraping, no spoofing involved.
 */
class VpnGateRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchServers(): Result<List<VpnServer>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://www.vpngate.net/api/iphone/")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                Result.success(parseCsv(body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCsv(raw: String): List<VpnServer> {
        val lines = raw.lines()
            .filterNot { it.startsWith("*") || it.startsWith("#") || it.isBlank() }

        return lines.mapNotNull { line ->
            val cols = line.split(",")
            if (cols.size < 15) return@mapNotNull null
            try {
                VpnServer(
                    hostName = cols[0],
                    ip = cols[1],
                    pingMs = cols[3].trim().toIntOrNull() ?: Int.MAX_VALUE,
                    speedBps = cols[4].trim().toLongOrNull() ?: 0L,
                    countryLong = cols[5],
                    countryShort = cols[6],
                    numSessions = cols[2].trim().toIntOrNull() ?: 0,
                    uptimeMs = cols[7].trim().toLongOrNull() ?: 0L,
                    openVpnConfigBase64 = cols[14].trim()
                )
            } catch (e: Exception) {
                null
            }
        }.filter { it.pingMs > 0 && it.openVpnConfigBase64.isNotBlank() }
            .sortedBy { it.pingMs }
    }

    /** Decodes the embedded base64 .ovpn profile for a server into raw config text. */
    fun decodeOvpnConfig(server: VpnServer): String =
        String(Base64.getDecoder().decode(server.openVpnConfigBase64))
}
