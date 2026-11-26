package com.astra.vpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.preference.PreferenceManager
import kittoku.osc.AstraStatus
import kittoku.osc.AstraStatusHolder
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.setBooleanPrefValue
import kittoku.osc.preference.accessor.setIntPrefValue
import kittoku.osc.preference.accessor.setStringPrefValue
import kittoku.osc.service.SstpVpnService

/**
 * Thin wrapper over the embedded SSTP engine (vendored from Open SSTP Client,
 * MIT, see kittoku/osc/LICENSE-Open-SSTP-Client.txt). VPN Gate relays accept
 * SSTP on the same TCP port as their SSL-VPN, with the public account vpn/vpn.
 */
class SstpEngine(private val context: Context) {

    val status get() = AstraStatusHolder.state

    /** Non-null when the system VPN consent dialog still has to be shown. */
    fun permissionIntent(): Intent? = VpnService.prepare(context)

    fun start(host: String, port: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        setStringPrefValue(host, OscPrefKey.HOME_HOSTNAME, prefs)
        setStringPrefValue("vpn", OscPrefKey.HOME_USERNAME, prefs)
        setStringPrefValue("vpn", OscPrefKey.HOME_PASSWORD, prefs)
        setIntPrefValue(port, OscPrefKey.SSL_PORT, prefs)
        // Public relays use self-signed certificates.
        setBooleanPrefValue(false, OscPrefKey.SSL_DO_VERIFY, prefs)
        setBooleanPrefValue(false, OscPrefKey.RECONNECTION_ENABLED, prefs)
        setBooleanPrefValue(true, OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE, prefs)

        AstraStatusHolder.set(AstraStatus.Connecting)
        val intent = Intent(context, SstpVpnService::class.java).setAction("kittoku.osc.connect")
        context.startForegroundService(intent)
    }

    fun stop() {
        val intent = Intent(context, SstpVpnService::class.java).setAction("kittoku.osc.disconnect")
        try {
            context.startService(intent)
        } catch (_: Throwable) {
        }
    }
}
