package com.astra.vpn

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astra.vpn.ui.components.AstraBackdrop
import com.astra.vpn.ui.components.AstraBottomBar
import com.astra.vpn.ui.components.AstraTab
import com.astra.vpn.ui.screens.HomeScreen
import com.astra.vpn.ui.screens.ServersScreen
import com.astra.vpn.ui.screens.SettingsScreen
import com.astra.vpn.ui.theme.AstraTheme
import com.astra.vpn.vpn.ConnectionViewModel

class MainActivity : ComponentActivity() {

    private val enginePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onEnginePermissionResult?.invoke() }

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
                            Toast.makeText(this, "درخواست مجوز VPN باز نشد", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AstraNavHost(
    onLaunchEnginePermission: (android.content.Intent, () -> Unit) -> Unit
) {
    val viewModel: ConnectionViewModel = viewModel()
    var currentTab by remember { mutableStateOf(AstraTab.HOME) }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var crashText by remember {
        mutableStateOf(
            java.io.File(appContext.filesDir, AstraApp.CRASH_FILE).takeIf { it.exists() }?.readText()
        )
    }
    crashText?.let { text ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("گزارش کرش قبلی") },
            text = {
                androidx.compose.foundation.rememberScrollState().let { sc ->
                    Column(Modifier.verticalScroll(sc)) { Text(text, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    java.io.File(appContext.filesDir, AstraApp.CRASH_FILE).delete()
                    crashText = null
                }) { Text("بستن") }
            }
        )
    }

    val connectionState by viewModel.connectionState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val serverLoadError by viewModel.serverLoadError.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val autoSelect by viewModel.autoSelect.collectAsState()
    val attemptInfo by viewModel.attemptInfo.collectAsState()
    val savedMirror by viewModel.mirrorHost.collectAsState()
    var mirrorDraft by remember(savedMirror) { mutableStateOf(savedMirror) }
    val permissionRequest by viewModel.permissionRequest.collectAsState()

    var splitTunnelOnlySelected by remember { mutableStateOf(false) }

    LaunchedEffect(permissionRequest) {
        permissionRequest?.let { intent ->
            onLaunchEnginePermission(intent) {
                viewModel.onVpnPermissionResult()
                viewModel.clearPermissionRequest()
            }
        }
    }

    AstraBackdrop {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = { AstraBottomBar(currentTab, { currentTab = it }) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentTab) {
                AstraTab.HOME -> HomeScreen(
                    connectionState = connectionState,
                    selectedServer = selectedServer,
                    attemptInfo = attemptInfo,
                    autoSelect = autoSelect,
                    onToggle = { viewModel.toggleConnection() },
                    onPickServer = { currentTab = AstraTab.SERVERS }
                )
                AstraTab.SERVERS -> ServersScreen(
                    servers = servers,
                    selectedServer = selectedServer,
                    loading = loading,
                    loadError = serverLoadError,
                    autoSelect = autoSelect,
                    onAutoSelectChange = { viewModel.setAutoSelect(it) },
                    onRefresh = { viewModel.refreshServers() },
                    onSelect = {
                        viewModel.setAutoSelect(false)
                        viewModel.selectServer(it)
                        currentTab = AstraTab.HOME
                    }
                )
                AstraTab.SETTINGS -> SettingsScreen(
                    mirrorHost = mirrorDraft,
                    onMirrorHostChange = { mirrorDraft = it },
                    onApplyMirror = {
                        viewModel.setMirrorHost(mirrorDraft)
                        viewModel.refreshServers()
                        currentTab = AstraTab.SERVERS
                    },
                    splitTunnelOnlySelected = splitTunnelOnlySelected,
                    onSplitTunnelToggle = { splitTunnelOnlySelected = it },
                    onManageApps = { /* TODO: app picker */ },
                    onBack = { currentTab = AstraTab.HOME }
                )
            }
        }
        }
    }
}
