package com.astra.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
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
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /**
     * Tries every source at the same time and returns the first that
     * works: the official host (often DNS-poisoned under national
     * filtering), the user's mirror, then built-in fallbacks. If all fail,
     * the error lists each source's failure so it can be diagnosed.
     */
    suspend fun fetchServers(mirrorHost: String? = null): Result<List<VpnServer>> =
        withContext(Dispatchers.IO) {
            val urls = buildList {
                add("http://www.vpngate.net/api/iphone/")
                mirrorHost?.trim()?.takeIf { it.isNotEmpty() }?.let { add(buildMirrorApiUrl(it)) }
                add("https://gate.sabaat.ir/api/iphone/")
                add("http://gate.sabaat.ir/api/iphone/")
            }.distinct()

            coroutineScope {
                val results = Channel<Pair<String, Result<List<VpnServer>>>>(urls.size)
                val jobs = urls.map { url ->
                    launch { results.send(url to fetchFrom(url)) }
                }
                val errors = mutableListOf<String>()
                repeat(urls.size) {
                    val (url, res) = results.receive()
                    if (res.isSuccess) {
                        jobs.forEach { it.cancel() }
                        return@coroutineScope res
                    }
                    errors += "${hostOf(url)}: ${res.exceptionOrNull()?.message ?: "خطا"}"
                }
                Result.failure(Exception(errors.joinToString("\n")))
            }
        }

    private fun hostOf(url: String) =
        Regex("^https?://([^/]+)").find(url)?.groupValues?.get(1) ?: url

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

    /** Cancellable fetch: cancelling the coroutine cancels the OkHttp call immediately. */
    private suspend fun fetchFrom(url: String): Result<List<VpnServer>> =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(Request.Builder().url(url).build())
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = try {
                        response.use {
                            if (!it.isSuccessful) {
                                Result.failure(Exception("HTTP ${it.code}"))
                            } else {
                                val servers = parseCsv(it.body?.string().orEmpty())
                                if (servers.isEmpty()) {
                                    Result.failure(Exception("پاسخ بدون سرور/کانفیگ"))
                                } else {
                                    Result.success(servers)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                    if (cont.isActive) cont.resume(result)
                }
            })
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
        String(Base64.getMimeDecoder().decode(server.openVpnConfigBase64))
}
