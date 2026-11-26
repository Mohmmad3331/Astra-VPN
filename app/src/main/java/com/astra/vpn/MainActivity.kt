package com.astra.vpn

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astra.vpn.ui.screens.HomeScreen
import com.astra.vpn.ui.screens.ServersScreen
import com.astra.vpn.ui.screens.SettingsScreen
import com.astra.vpn.ui.theme.AstraSurface
import com.astra.vpn.ui.theme.AstraTheme
import com.astra.vpn.vpn.ConnectionViewModel

class MainActivity : ComponentActivity() {

    private val enginePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onEnginePermissionResult?.invoke() }

    private val installEngineLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* user returns from Play Store either way */ }

    private var onEnginePermissionResult: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstraTheme {
                AstraNavHost(
                    onLaunchEnginePermission = { intent, onGranted ->
                        onEnginePermissionResult = onGranted
                        try {
                            enginePermissionLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(this, "اپ OpenVPN for Android پیدا نشد", Toast.LENGTH_LONG).show()
                        }
                    },
                    onLaunchInstallEngine = { intent ->
                        try {
                            installEngineLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                this,
                                "مرورگر یا فروشگاهی برای باز کردن لینک پیدا نشد. نصب کن: de.blinkt.openvpn",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

private enum class Tab(val label: String) { HOME("خانه"), SERVERS("سرورها"), SETTINGS("تنظیمات") }

@Composable
private fun AstraNavHost(
    onLaunchEnginePermission: (android.content.Intent, () -> Unit) -> Unit,
    onLaunchInstallEngine: (android.content.Intent) -> Unit
) {
    val viewModel: ConnectionViewModel = viewModel()
    var currentTab by remember { mutableStateOf(Tab.HOME) }
    var showInstallEngineDialog by remember { mutableStateOf(false) }

    val connectionState by viewModel.connectionState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val serverLoadError by viewModel.serverLoadError.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val savedMirror by viewModel.mirrorHost.collectAsState()
    var mirrorDraft by remember(savedMirror) { mutableStateOf(savedMirror) }
    val permissionRequest by viewModel.permissionRequest.collectAsState()

    var splitTunnelOnlySelected by remember { mutableStateOf(false) }

    LaunchedEffect(permissionRequest) {
        permissionRequest?.let { intent ->
            onLaunchEnginePermission(intent) {
                viewModel.onEnginePermissionGranted()
                viewModel.clearPermissionRequest()
            }
        }
    }

    if (showInstallEngineDialog) {
        AlertDialog(
            onDismissRequest = { showInstallEngineDialog = false },
            title = { Text("نیاز به موتور اتصال") },
            text = { Text("Astra VPN برای برقراری تونل واقعی از اپ متن‌باز «OpenVPN for Android» استفاده می‌کند. لطفاً یک‌بار آن را از F-Droid نصب کنید (نه گوگل پلی، که از ایران در دسترس نیست).") },
            confirmButton = {
                TextButton(onClick = {
                    showInstallEngineDialog = false
                    onLaunchInstallEngine(viewModel.installEngineAppIntent())
                }) { Text("نصب") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallEngineDialog = false }) { Text("بعداً") }
            }
        )
    }

    Scaffold(
        containerColor = AstraSurface,
        bottomBar = {
            NavigationBar(containerColor = AstraSurface) {
                NavigationBarItem(
                    selected = currentTab == Tab.HOME,
                    onClick = { currentTab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                    label = { Text(Tab.HOME.label) }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.SERVERS,
                    onClick = { currentTab = Tab.SERVERS },
                    icon = { Icon(Icons.Filled.Public, contentDescription = null) },
                    label = { Text(Tab.SERVERS.label) }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.SETTINGS,
                    onClick = { currentTab = Tab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(Tab.SETTINGS.label) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentTab) {
                Tab.HOME -> HomeScreen(
                    connectionState = connectionState,
                    selectedServer = selectedServer,
                    onToggle = {
                        if (!viewModel.engineAppInstalled) {
                            showInstallEngineDialog = true
                        } else {
                            viewModel.toggleConnection()
                        }
                    },
                    onPickServer = { currentTab = Tab.SERVERS }
                )
                Tab.SERVERS -> ServersScreen(
                    servers = servers,
                    selectedServer = selectedServer,
                    loading = loading,
                    loadError = serverLoadError,
                    onRefresh = { viewModel.refreshServers() },
                    onSelect = {
                        viewModel.selectServer(it)
                        currentTab = Tab.HOME
                    }
                )
                Tab.SETTINGS -> SettingsScreen(
                    mirrorHost = mirrorDraft,
                    onMirrorHostChange = { mirrorDraft = it },
                    onApplyMirror = {
                        viewModel.setMirrorHost(mirrorDraft)
                        viewModel.refreshServers()
                        currentTab = Tab.SERVERS
                    },
                    splitTunnelOnlySelected = splitTunnelOnlySelected,
                    onSplitTunnelToggle = { splitTunnelOnlySelected = it },
                    onManageApps = { /* TODO: app picker */ }
                )
            }
        }
    }
}
