package com.astra.vpn.vpn

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astra.vpn.data.VpnGateRepository
import com.astra.vpn.data.VpnServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = VpnGateRepository()
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

    init {
        refreshServers()
    }

    fun refreshServers() {
        viewModelScope.launch {
            _loading.value = true
            repo.fetchServers().onSuccess { list ->
                _servers.value = list
                if (_selectedServer.value == null) {
                    _selectedServer.value = list.firstOrNull()
                }
            }.onFailure {
                _connectionState.value = ConnectionState.Error(it.message ?: "خطا در دریافت لیست سرورها")
            }
            _loading.value = false
        }
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
    }

    fun toggleConnection() {
        val server = _selectedServer.value ?: return
        if (engine.isConnected()) {
            engine.disconnect()
            _connectionState.value = ConnectionState.Disconnected
        } else {
            val config = repo.decodeOvpnConfig(server)
            engine.connect(server, config) { state ->
                _connectionState.value = state
                engine.pendingPermissionIntent?.let { _permissionRequest.value = it }
            }
        }
    }
}
