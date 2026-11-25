package com.astra.vpn.vpn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.astra.vpn.data.VpnServer
import de.blinkt.openvpn.api.IOpenVPNAPIService
import de.blinkt.openvpn.api.IOpenVPNStatusCallback

/**
 * Real tunneling, delegated to the official "OpenVPN for Android" app
 * (package de.blinkt.openvpn, by the ics-openvpn project) through its
 * public External OpenVPN API (a lightweight AIDL contract meant for
 * exactly this — no native engine embedded in Astra VPN itself).
 *
 * That app must be installed once (free, open-source, Play Store or
 * F-Droid). Astra VPN fetches the server list and .ovpn config, then
 * asks that app's engine to actually open the tunnel.
 */
const val OPENVPN_PACKAGE = "de.blinkt.openvpn"
private const val API_ACTION = "de.blinkt.openvpn.api.IOpenVPNAPIService"

class OpenVpnEngineBridge(private val appContext: Context) : VpnEngineBridge {

    private var service: IOpenVPNAPIService? = null
    private var onStateCallback: ((ConnectionState) -> Unit)? = null
    private var currentFlag: String = ""
    private var connectedSince: Long = 0L
    private var pendingConfig: String? = null
    private var pendingServerName: String = ""

    private val statusCallback = object : IOpenVPNStatusCallback.Stub() {
        override fun newStatus(uuid: String?, state: String?, message: String?, level: String?) {
            val mapped = when (state) {
                "CONNECTED" -> {
                    if (connectedSince == 0L) connectedSince = System.currentTimeMillis()
                    ConnectionState.Connected(pendingServerName, currentFlag, connectedSince)
                }
                "NOPROCESS", "DISCONNECTED", "AUTH_FAILED" -> {
                    connectedSince = 0L
                    if (state == "AUTH_FAILED") ConnectionState.Error("خطای احراز هویت با سرور")
                    else ConnectionState.Disconnected
                }
                else -> ConnectionState.Connecting
            }
            onStateCallback?.invoke(mapped)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IOpenVPNAPIService.Stub.asInterface(binder)
            service?.registerStatusCallback(statusCallback)
            val config = pendingConfig
            if (config != null) {
                pendingConfig = null
                startAfterBind(config)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    /** Checks whether the OpenVPN for Android engine app is installed. */
    fun isEngineAppInstalled(): Boolean = try {
        appContext.packageManager.getPackageInfo(OPENVPN_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun installEngineAppIntent(): Intent =
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$OPENVPN_PACKAGE"))

    private fun bindIfNeeded() {
        if (service != null) return
        val intent = Intent(API_ACTION).apply { setPackage(OPENVPN_PACKAGE) }
        appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun connect(server: VpnServer, ovpnConfig: String, onState: (ConnectionState) -> Unit) {
        onStateCallback = onState
        currentFlag = server.flagEmoji
        pendingServerName = server.countryLong
        connectedSince = 0L

        if (!isEngineAppInstalled()) {
            onState(ConnectionState.Error("ابتدا اپ «OpenVPN for Android» را نصب کنید"))
            return
        }

        onState(ConnectionState.Connecting)
        val svc = service
        if (svc == null) {
            pendingConfig = ovpnConfig
            bindIfNeeded()
        } else {
            startAfterBind(ovpnConfig)
        }
    }

    private fun startAfterBind(ovpnConfig: String) {
        val svc = service ?: return
        try {
            // Step 1 — confused-deputy guard: the engine app must explicitly
            // allow this app to control it at all.
            val appPermissionIntent = svc.prepare(appContext.packageName)
            if (appPermissionIntent != null) {
                requestPermission(appPermissionIntent) { startAfterBind(ovpnConfig) }
                return
            }
            // Step 2 — the system-level VpnService consent dialog, requested
            // through the engine app since it owns the TUN interface.
            val vpnServiceIntent = svc.prepareVPNService()
            if (vpnServiceIntent != null) {
                requestPermission(vpnServiceIntent) { startAfterBind(ovpnConfig) }
                return
            }
            svc.startVPN(ovpnConfig)
        } catch (e: Exception) {
            onStateCallback?.invoke(ConnectionState.Error(e.message ?: "اتصال برقرار نشد"))
        }
    }

    private fun requestPermission(intent: Intent, onGranted: () -> Unit) {
        onStateCallback?.invoke(ConnectionState.Error("لطفاً دسترسی لازم را تأیید کنید"))
        pendingPermissionAction = onGranted
        pendingPermissionIntent = intent
    }

    // Exposed so the Activity can relaunch the permission flow and retry.
    var pendingPermissionIntent: Intent? = null
        private set
    private var pendingPermissionAction: (() -> Unit)? = null

    fun onPermissionGranted() {
        pendingPermissionAction?.invoke()
        pendingPermissionAction = null
        pendingPermissionIntent = null
    }

    override fun disconnect() {
        try {
            service?.disconnect()
        } catch (e: Exception) {
            // already disconnected
        }
        connectedSince = 0L
    }

    override fun isConnected(): Boolean = connectedSince != 0L
}
