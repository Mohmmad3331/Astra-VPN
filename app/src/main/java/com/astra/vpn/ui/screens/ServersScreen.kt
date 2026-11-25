package com.astra.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astra.vpn.data.VpnServer
import com.astra.vpn.ui.theme.*

@Composable
fun ServersScreen(
    servers: List<VpnServer>,
    selectedServer: VpnServer?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onSelect: (VpnServer) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AstraBackground)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سرورها", style = MaterialTheme.typography.headlineMedium, color = AstraTextPrimary)
            TextButton(onClick = onRefresh) {
                Text(if (loading) "در حال بروزرسانی…" else "بروزرسانی", color = AstraCyan)
            }
        }

        if (loading && servers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AstraIndigo)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(servers) { server ->
                    ServerRow(
                        server = server,
                        isSelected = server.ip == selectedServer?.ip,
                        onClick = { onSelect(server) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ServerRow(server: VpnServer, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) AstraIndigo.copy(alpha = 0.16f) else AstraSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AstraSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(server.flagEmoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(server.countryLong, color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.SignalCellularAlt,
                    contentDescription = null,
                    tint = pingColor(server.pingMs),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${server.pingMs} ms · ${"%.1f".format(server.speedMbps)} Mbps · ${server.numSessions} کاربر",
                    color = AstraTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AstraGreen)
        }
    }
}

private fun pingColor(ping: Int) = when {
    ping < 150 -> AstraGreen
    ping < 350 -> Color_Warn
    else -> AstraRed
}

private val Color_Warn = androidx.compose.ui.graphics.Color(0xFFFFC857)
