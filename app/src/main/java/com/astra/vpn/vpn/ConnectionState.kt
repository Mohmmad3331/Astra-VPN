package com.astra.vpn.vpn

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val serverName: String, val countryFlag: String, val sinceMillis: Long) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
