package com.astra.vpn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astra.vpn.data.VpnServer
import com.astra.vpn.ui.components.*
import com.astra.vpn.ui.theme.*
import com.astra.vpn.vpn.ConnectionState

@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    selectedServer: VpnServer?,
    attemptInfo: String? = null,
    autoSelect: Boolean = true,
    onToggle: () -> Unit,
    onPickServer: () -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val powerSize = (maxHeight * 0.30f).coerceIn(140.dp, 210.dp)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Astra VPN",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                color = AstraTextPrimary
            )
            Spacer(Modifier.height(10.dp))
            StatusPill(connectionState)

            Spacer(Modifier.weight(1f))
            PowerButton(isConnected, isConnecting, powerSize, onToggle)
            Spacer(Modifier.height(20.dp))

            GoldPill(
                text = when {
                    isConnected -> "متصل"
                    isConnecting -> "در حال اتصال…"
                    else -> "غیرفعال"
                },
                textColor = if (isConnected) AstraGreen else AstraTextPrimary,
                modifier = Modifier.scale(1.25f)
            )

            if (attemptInfo != null) {
                Spacer(Modifier.height(18.dp))
                Text(attemptInfo, color = AstraTextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            if (connectionState is ConnectionState.Error) {
                Spacer(Modifier.height(18.dp))
                Text(
                    connectionState.message,
                    color = AstraRed,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))
            ServerCard(selectedServer, autoSelect && !isConnected, onPickServer)
            Spacer(Modifier.height(12.dp))
            QuickConnectBanner(isConnected || isConnecting, onToggle)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusPill(state: ConnectionState) {
    val (label, color) = when (state) {
        is ConnectionState.Connected -> "امن" to AstraGreen
        is ConnectionState.Connecting -> "در حال برقراری" to AstraGold
        is ConnectionState.Error -> "خطا" to AstraRed
        else -> "بدون محافظت" to AstraTextSecondary
    }
    GoldPill(text = label, dot = color)
}

@Composable
private fun PowerButton(isConnected: Boolean, isConnecting: Boolean, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "power")
    val pulse by transition.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "spin"
    )
    val glow = if (isConnected || isConnecting) pulse else 1f
    val ringAngle = if (isConnecting) spin else 0f

    Box(Modifier.size(size * 1.25f), contentAlignment = Alignment.Center) {
        // Soft golden aura.
        Box(
            Modifier
                .fillMaxSize()
                .scale(glow)
                .background(
                    Brush.radialGradient(
                        listOf(
                            (if (isConnected) AstraGreen else AstraGold).copy(alpha = if (isConnected) 0.28f else 0.32f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        // Golden ring (rotates while connecting).
        Canvas(Modifier.size(size)) {
            val stroke = 6.dp.toPx()
            rotate(ringAngle) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(AstraGoldLight, AstraGold, AstraGoldDeep.copy(alpha = 0.2f), AstraGold, AstraGoldLight)
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(stroke)
                )
            }
        }
        // Glass disc.
        Box(
            Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF4A4A4A), Color(0xFF141414), Color(0xFF000000)))
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = "اتصال / قطع",
                tint = if (isConnected) AstraGoldLight else AstraGold,
                modifier = Modifier.size(size * 0.38f)
            )
        }
    }
}

@Composable
private fun ServerCard(server: VpnServer?, showAuto: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FlagCircle(if (showAuto) null else server?.flagEmoji)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (showAuto) "انتخاب خودکار" else server?.countryLong ?: "در حال یافتن بهترین سرور…",
                    color = AstraTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                if (showAuto) {
                    Text(
                        "سریع‌ترین سرور موجود انتخاب می‌شود",
                        color = AstraTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (server != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SignalBars(server.pingMs)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${server.pingMs} ms · ${"%.1f".format(server.speedMbps)} Mbps",
                            color = AstraTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Icon(Icons.Filled.SwapHoriz, null, tint = AstraGold, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text("تغییر", color = AstraGold, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun FlagCircle(flag: String?) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, AstraGold.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (flag != null) Text(flag, fontSize = 22.sp)
        else Icon(Icons.Filled.Public, null, tint = AstraGold)
    }
}

@Composable
private fun QuickConnectBanner(active: Boolean, onClick: () -> Unit) {
    GlassCard(
        Modifier.fillMaxWidth(),
        selected = true,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bolt, null, tint = AstraGold, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (active) "قطع اتصال" else "اتصال سریع و پایدار",
                    color = AstraTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "با Astra VPN همیشه آنلاین بمانید",
                    color = AstraTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(Icons.Filled.ChevronLeft, null, tint = AstraGold)
        }
    }
}
