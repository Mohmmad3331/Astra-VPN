package com.astra.vpn.vpn

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astra.vpn.data.VpnGateRepository
import com.astra.vpn.data.VpnServer
import kittoku.osc.AstraStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = VpnGateRepository()
    private val prefs = application.getSharedPreferences("astra_prefs", android.content.Context.MODE_PRIVATE)

    // Mirror host used when www.vpngate.net is filtered. Defaults to a
    // node mirror that was reachable; rotates over time, so it's editable.
    private val _mirrorHost = MutableStateFlow(
        prefs.getString("mirror_host", DEFAULT_MIRROR) ?: DEFAULT_MIRROR
    )
    val mirrorHost: StateFlow<String> = _mirrorHost

    fun setMirrorHost(value: String) {
        _mirrorHost.value = value
        prefs.edit().putString("mirror_host", value).apply()
    }

    private val engine = SstpEngine(application.applicationContext)

    private val _permissionRequest = MutableStateFlow<Intent?>(null)
    val permissionRequest: StateFlow<Intent?> = _permissionRequest

    fun clearPermissionRequest() {
        _permissionRequest.value = null
    }

    /** Called after the system VPN consent dialog closes. */
    fun onVpnPermissionResult() {
        if (engine.permissionIntent() == null) {
            toggleConnection()
        } else {
            _connectionState.value = ConnectionState.Error("مجوز VPN داده نشد.")
        }
    }

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _autoSelect = MutableStateFlow(prefs.getBoolean("auto_select", true))
    val autoSelect: StateFlow<Boolean> = _autoSelect

    fun setAutoSelect(value: Boolean) {
        _autoSelect.value = value
        prefs.edit().putBoolean("auto_select", value).apply()
    }

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer

    private val _serverLoadError = MutableStateFlow<String?>(null)
    val serverLoadError: StateFlow<String?> = _serverLoadError

    init {
        refreshServers()
    }

    fun refreshServers() {
        viewModelScope.launch {
            _loading.value = true
            _serverLoadError.value = null
            repo.fetchServers(_mirrorHost.value).onSuccess { list ->
                _servers.value = list
                if (_selectedServer.value == null) {
                    _selectedServer.value = list.firstOrNull()
                }
                if (list.isEmpty()) {
                    _serverLoadError.value = "لیست سرور خالی برگشت. دوباره تلاش کن."
                }
            }.onFailure {
                _serverLoadError.value = "دریافت لیست سرورها ناموفق بود: ${it.message ?: it::class.simpleName}"
            }
            _loading.value = false
        }
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
    }

    private var connectJob: Job? = null

    private val _attemptInfo = MutableStateFlow<String?>(null)
    val attemptInfo: StateFlow<String?> = _attemptInfo

    fun toggleConnection() {
        val state = _connectionState.value
        if (state is ConnectionState.Connected || state is ConnectionState.Connecting || connectJob?.isActive == true) {
            connectJob?.cancel()
            connectJob = null
            _attemptInfo.value = null
            engine.stop()
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        if (_selectedServer.value == null) return

        engine.permissionIntent()?.let {
            _permissionRequest.value = it
            return
        }

        connectJob = viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            val picked = _selectedServer.value!!
            val candidates = if (_autoSelect.value) {
                // Auto: current pick first, then the fastest TCP (SSTP-capable) relays, at most MAX_ATTEMPTS.
                val others = _servers.value
                    .filter { it.ip != picked.ip && it.pingMs < 500 && it.transport?.first == "tcp" }
                    .sortedWith(compareBy({ it.filterResistanceRank }, { -it.speedBps }))
                (listOf(picked) + others).take(MAX_ATTEMPTS)
            } else {
                // Manual: only the server the user picked.
                listOf(picked)
            }

            for ((i, server) in candidates.withIndex()) {
                _selectedServer.value = server
                val port = server.transport?.takeIf { it.first == "tcp" }?.second ?: 443
                _attemptInfo.value = if (candidates.size > 1) "تلاش ${i + 1} از ${candidates.size} · ${server.countryLong}" else server.countryLong
                val ok = try {
                    engine.start(server.ip, port)
                    awaitConnected()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    false
                }
                if (ok) {
                    _attemptInfo.value = null
                    _connectionState.value = ConnectionState.Connected(
                        server.countryLong.ifBlank { server.hostName }, server.flagEmoji, System.currentTimeMillis()
                    )
                    watchConnection(server)
                    return@launch
                }
                engine.stop()
                delay(1500)
            }
            _attemptInfo.value = null
            _connectionState.value = ConnectionState.Error(
                "هیچ‌کدام از ${candidates.size} سرور جواب ندادند. دوباره امتحان کن یا لیست را بروزرسانی کن."
            )
        }
    }

    private suspend fun awaitConnected(): Boolean {
        var waitedMs = 0L
        while (waitedMs < ATTEMPT_TIMEOUT_MS) {
            when (engine.status.value) {
                is AstraStatus.Connected -> return true
                is AstraStatus.Failed -> return false
                else -> {}
            }
            delay(300)
            waitedMs += 300
        }
        return false
    }

    /** After success, reflect later drops (server closed, network lost) in the UI. */
    private fun watchConnection(server: VpnServer) {
        viewModelScope.launch {
            val st = engine.status.first { it is AstraStatus.Failed || it is AstraStatus.Idle }
            if (_connectionState.value is ConnectionState.Connected) {
                _connectionState.value = if (st is AstraStatus.Failed)
                    ConnectionState.Error("اتصال قطع شد (${st.reason})")
                else ConnectionState.Disconnected
            }
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 6
        const val ATTEMPT_TIMEOUT_MS = 25_000L
        const val DEFAULT_MIRROR = "http://v-dot-pn-ams2.opengw.net:34637"
    }
}
