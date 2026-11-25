package com.astra.vpn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astra.vpn.data.VpnServer
import com.astra.vpn.ui.theme.*
import com.astra.vpn.vpn.ConnectionState

@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    selectedServer: VpnServer?,
    onToggle: () -> Unit,
    onPickServer: () -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AstraBackground, Color(0xFF0E1220), AstraBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "Astra VPN",
                style = MaterialTheme.typography.headlineLarge,
                color = AstraTextPrimary
            )

            Spacer(Modifier.height(4.dp))

            StatusPill(connectionState)

            Spacer(Modifier.weight(1f))

            PowerButton(
                isConnected = isConnected,
                isConnecting = isConnecting,
                onClick = onToggle
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = when {
                    isConnected -> "متصل"
                    isConnecting -> "در حال اتصال…"
                    else -> "غیرفعال"
                },
                style = MaterialTheme.typography.titleLarge,
                color = AstraTextPrimary
            )

            Spacer(Modifier.height(32.dp))

            Spacer(Modifier.weight(1f))

            ServerPickerCard(
                server = selectedServer,
                onClick = onPickServer
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusPill(state: ConnectionState) {
    val (label, color) = when (state) {
        is ConnectionState.Connected -> "امن" to AstraGreen
        is ConnectionState.Connecting -> "در حال برقراری" to AstraCyan
        is ConnectionState.Error -> "خطا" to AstraRed
        else -> "بدون محافظت" to AstraTextSecondary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PowerButton(isConnected: Boolean, isConnecting: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val accentColor = if (isConnected) AstraGreen else AstraIndigo
    val glowScale = if (isConnected || isConnecting) pulse else 1f

    Box(contentAlignment = Alignment.Center) {
        // Outer glow rings
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accentColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(176.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(AstraIndigo, AstraCyan, AstraIndigo)
                    )
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(AstraSurface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = "اتصال / قطع",
                tint = if (isConnected) AstraGreen else AstraTextPrimary,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun ServerPickerCard(server: VpnServer?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AstraSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AstraIndigo.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (server != null) {
                Text(server.flagEmoji, fontSize = 20.sp)
            } else {
                Icon(Icons.Filled.Public, contentDescription = null, tint = AstraIndigo)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                server?.countryLong ?: "در حال یافتن بهترین سرور…",
                color = AstraTextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            if (server != null) {
                Text(
                    "پینگ ${server.pingMs} ms · ${"%.1f".format(server.speedMbps)} Mbps",
                    color = AstraTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text("تغییر", color = AstraCyan, fontWeight = FontWeight.Medium)
    }
}
