package com.astra.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Fetches the public server list from the VPN Gate academic project's
 * official CSV API (http://www.vpngate.net/api/iphone/) — publicly
 * documented, open, no account, no scraping, no spoofing involved.
 *
 * www.vpngate.net itself can be blocked by national filtering. VPN Gate's
 * own anti-censorship design accounts for this: every public relay node
 * also mirrors the exact same CSV at its own host/port, under the same
 * /api/iphone/ path — these "mirror URLs" rotate daily and get posted to
 * VPN Gate's own mailing list / Google Groups for people whose access to
 * the main site is blocked. If the primary host fails, this falls back
 * to a mirror host the user supplies in Settings.
 */
class VpnGateRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchServers(mirrorHost: String? = null): Result<List<VpnServer>> =
        withContext(Dispatchers.IO) {
            val primary = fetchFrom("http://www.vpngate.net/api/iphone/")
            if (primary.isSuccess) return@withContext primary

            val mirror = mirrorHost?.trim()?.takeIf { it.isNotEmpty() }
            if (mirror == null) return@withContext primary

            val mirrorUrl = buildMirrorApiUrl(mirror)
            val fromMirror = fetchFrom(mirrorUrl)
            if (fromMirror.isSuccess) fromMirror else primary
        }

    /** Turns whatever the user pasted (full URL, host:port, bare host) into an /api/iphone/ URL. */
    private fun buildMirrorApiUrl(input: String): String {
        var url = input
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        // Strip any path the user included (e.g. the node's "/en/" status page)
        // and point at the mirrored CSV endpoint instead.
        val schemeAndAuthority = Regex("^(https?://[^/]+)").find(url)?.groupValues?.get(1) ?: url
        return "$schemeAndAuthority/api/iphone/"
    }

    private fun fetchFrom(url: String): Result<List<VpnServer>> {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP ${response.code} از $url"))
                }
                val body = response.body?.string().orEmpty()
                val servers = parseCsv(body)
                if (servers.isEmpty()) {
                    Result.failure(Exception("پاسخ خالی از $url"))
                } else {
                    Result.success(servers)
                }
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
