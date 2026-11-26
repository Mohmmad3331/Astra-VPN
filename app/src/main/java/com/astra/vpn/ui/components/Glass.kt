package com.astra.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.astra.vpn.ui.theme.*

/** Full-screen black backdrop with soft golden light leaks (cheap gradients, no real blur). */
@Composable
fun AstraBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AstraBlack)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x40FFC043), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.02f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x30F59E0B), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.98f),
                        radius = size.width * 0.9f
                    )
                )
            },
        content = content
    )
}

/** Translucent glass panel with a glowing gold edge. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val edge by animateColorAsState(
        if (selected) AstraGold else AstraGold.copy(alpha = 0.35f),
        animationSpec = tween(250), label = "edge"
    )
    val fill by animateColorAsState(
        if (selected) Color(0x33FFC043) else Color.Transparent,
        animationSpec = tween(250), label = "fill"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(AstraGlassBrush)
            .background(fill)
            .border(BorderStroke(if (selected) 1.6.dp else 1.dp, edge), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(contentPadding),
        content = content
    )
}

/** Round dark badge holding a golden icon. */
@Composable
fun GoldIconBadge(icon: ImageVector, size: Dp = 44.dp, contentDescription: String? = null, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2112), Color(0xFF0C0A06))))
            .border(1.dp, AstraGold.copy(alpha = 0.6f), CircleShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = AstraGold, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun FlagBadge(flag: String?, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF0C0A06))
            .border(1.dp, AstraGold.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (flag != null) Text(flag, fontSize = (size.value * 0.5f).sp)
        else Icon(Icons.Filled.Public, null, tint = AstraGold, modifier = Modifier.size(size * 0.5f))
    }
}

/** 3-step signal indicator: [level] 1..3. */
@Composable
fun SignalBars(level: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..3) {
            Box(
                Modifier
                    .width(4.dp)
                    .height((5 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= level) color else Color(0x33FFFFFF))
            )
        }
    }
}

fun signalLevel(pingMs: Int) = when {
    pingMs < 150 -> 3
    pingMs < 350 -> 2
    else -> 1
}

fun signalColor(pingMs: Int) = when {
    pingMs < 150 -> AstraGreen
    pingMs < 350 -> AstraWarn
    else -> AstraRed
}

@Composable
fun GoldTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = AstraTextPrimary, style = MaterialTheme.typography.headlineMedium)
}

enum class AstraTab(val label: String, val icon: ImageVector) {
    HOME("خانه", Icons.Filled.Shield),
    SERVERS("سرورها", Icons.Filled.Public),
    SETTINGS("تنظیمات", Icons.Filled.Settings)
}

@Composable
fun AstraBottomNav(selected: AstraTab, onSelect: (AstraTab) -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AstraTab.values().forEach { tab ->
                val isSel = tab == selected
                val tint by animateColorAsState(
                    if (isSel) AstraGold else AstraTextSecondary, tween(200), label = "navTint"
                )
                val bg by animateColorAsState(
                    if (isSel) Color(0x33FFC043) else Color.Transparent, tween(200), label = "navBg"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .border(
                            if (isSel) BorderStroke(1.dp, AstraGold.copy(alpha = 0.7f)) else BorderStroke(0.dp, Color.Transparent),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(tab.icon, null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(tab.label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
