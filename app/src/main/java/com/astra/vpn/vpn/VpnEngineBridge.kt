package com.astra.vpn.vpn

import com.astra.vpn.data.VpnServer

/**
 * Abstraction over the actual tunneling engine, implemented by
 * OpenVpnEngineBridge (see that file) via the "OpenVPN for Android"
 * app's External OpenVPN API.
 */
interface VpnEngineBridge {
    fun connect(server: VpnServer, ovpnConfig: String, onState: (ConnectionState) -> Unit)
    fun disconnect()
    fun isConnected(): Boolean
}
