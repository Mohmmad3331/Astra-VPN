package com.astra.vpn.vpn

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astra.vpn.data.VpnGateRepository
import com.astra.vpn.data.VpnServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val engine = OpenVpnEngineBridge(application.applicationContext)

    val engineAppInstalled: Boolean get() = engine.isEngineAppInstalled()
    fun installEngineAppIntent() = engine.installEngineAppIntent()
    fun onEnginePermissionGranted() = engine.onPermissionGranted()

    private val _permissionRequest = MutableStateFlow<Intent?>(null)
    val permissionRequest: StateFlow<Intent?> = _permissionRequest

    fun clearPermissionRequest() {
        _permissionRequest.value = null
    }

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

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
        if (engine.isConnected() || state is ConnectionState.Connecting || connectJob?.isActive == true) {
            connectJob?.cancel()
            connectJob = null
            _attemptInfo.value = null
            try { engine.disconnect() } catch (_: Throwable) {}
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        if (_selectedServer.value == null) return

        connectJob = viewModelScope.launch {
            // User's pick first, then the most filter-resistant fast servers.
            val picked = _selectedServer.value!!
            val others = _servers.value
                .filter { it.ip != picked.ip && it.pingMs < 500 }
                .sortedWith(compareBy({ it.filterResistanceRank }, { -it.speedBps }))
            val candidates = (listOf(picked) + others).take(MAX_ATTEMPTS)

            for ((i, server) in candidates.withIndex()) {
                _selectedServer.value = server
                _attemptInfo.value = "تلاش ${i + 1} از ${candidates.size} · ${server.countryLong}" +
                    (server.transport?.let { " (${it.first.uppercase()}:${it.second})" } ?: "")
                val ok = try {
                    val config = repo.decodeOvpnConfig(server)
                    engine.connect(server, config) { st ->
                        _connectionState.value = st
                        engine.pendingPermissionIntent?.let { _permissionRequest.value = it }
                    }
                    awaitConnected()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    false
                }
                if (ok) {
                    _attemptInfo.value = null
                    return@launch
                }
                try { engine.disconnect() } catch (_: Throwable) {}
                delay(1200)
            }
            _attemptInfo.value = null
            _connectionState.value = ConnectionState.Error(
                "هیچ‌کدام از $MAX_ATTEMPTS سرور جواب ندادند. دوباره امتحان کن یا لیست را بروزرسانی کن."
            )
        }
    }

    /** Waits for Connected; the clock is paused while a permission dialog is pending. */
    private suspend fun awaitConnected(): Boolean {
        var waitedMs = 0L
        while (waitedMs < ATTEMPT_TIMEOUT_MS) {
            val st = _connectionState.value
            if (st is ConnectionState.Connected) return true
            val waitingForUser = engine.pendingPermissionIntent != null
            if (st is ConnectionState.Error && !waitingForUser) return false
            delay(500)
            if (!waitingForUser) waitedMs += 500
        }
        return false
    }

    companion object {
        const val MAX_ATTEMPTS = 6
        const val ATTEMPT_TIMEOUT_MS = 25_000L
        const val DEFAULT_MIRROR = "http://v-dot-pn-ams2.opengw.net:34637"
    }
}
