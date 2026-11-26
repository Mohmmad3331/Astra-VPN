package com.astra.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astra.vpn.data.VpnServer
import com.astra.vpn.ui.components.*
import com.astra.vpn.ui.theme.*

@Composable
fun ServersScreen(
    servers: List<VpnServer>,
    selectedServer: VpnServer?,
    loading: Boolean,
    loadError: String?,
    onRefresh: () -> Unit,
    onSelect: (VpnServer) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("سرورها", Icons.Filled.Public) {
            TextButton(onClick = onRefresh, enabled = !loading) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = AstraGold, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, null, tint = AstraGold, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(if (loading) "در حال بروزرسانی…" else "بروزرسانی", color = AstraGold)
            }
        }

        if (loading && servers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AstraGold)
            }
        } else if (servers.isEmpty() && loadError != null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(loadError, color = AstraRed, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onRefresh) { Text("تلاش دوباره", color = AstraGold) }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(servers, key = { it.ip }) { server ->
                    ServerRow(server, server.ip == selectedServer?.ip) { onSelect(server) }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ServerRow(server: VpnServer, isSelected: Boolean, onClick: () -> Unit) {
    GlassCard(
        Modifier.fillMaxWidth(),
        selected = isSelected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FlagCircle(server.flagEmoji)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(server.countryLong, color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    "ID: ${server.hostName}",
                    color = AstraTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Text(
                    "${server.pingMs} ms · ${"%.1f".format(server.speedMbps)} Mbps · ${server.numSessions} کاربر",
                    color = AstraTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SignalBars(server.pingMs)
            Spacer(Modifier.width(10.dp))
            if (isSelected) {
                Icon(Icons.Filled.CheckCircle, null, tint = AstraGold)
            } else {
                Icon(Icons.Filled.ChevronLeft, null, tint = AstraGold.copy(alpha = 0.8f))
            }
        }
    }
}
