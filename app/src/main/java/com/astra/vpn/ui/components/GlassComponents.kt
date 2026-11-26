package com.astra.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astra.vpn.ui.theme.*

val GlassShape = RoundedCornerShape(22.dp)

/** Black canvas with soft golden glows and light streaks. No blur: gradients only. */
@Composable
fun AstraBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(AstraBlack)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(AstraGold.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.1f, 0f),
                        radius = size.width * 0.95f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(AstraGold.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height),
                        radius = size.width * 0.9f
                    )
                )
                val streak = Path().apply {
                    moveTo(0f, size.height * 0.62f)
                    cubicTo(
                        size.width * 0.3f, size.height * 0.52f,
                        size.width * 0.6f, size.height * 0.78f,
                        size.width, size.height * 0.66f
                    )
                }
                drawPath(
                    streak,
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, AstraGold.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    style = Stroke(width = 2f)
                )
            },
        content = content
    )
}

/** Translucent glass card with a soft golden border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = GlassShape,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val borderTop by animateColorAsState(
        if (selected) AstraGoldLight else AstraGold.copy(alpha = 0.75f), label = "borderTop"
    )
    val borderBottom by animateColorAsState(
        if (selected) AstraGold else AstraGold.copy(alpha = 0.18f), label = "borderBottom"
    )
    val fill = if (selected) {
        Brush.verticalGradient(listOf(AstraGold.copy(alpha = 0.20f), AstraGold.copy(alpha = 0.05f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.02f)))
    }
    Box(
        modifier
            .clip(shape)
            .background(AstraCharcoal.copy(alpha = 0.85f))
            .background(fill)
            .border(
                BorderStroke(if (selected) 1.6.dp else 1.dp, Brush.verticalGradient(listOf(borderTop, borderBottom))),
                shape
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(contentPadding),
        content = content
    )
}

/** Round dark badge with a gold icon. */
@Composable
fun GoldBadge(icon: ImageVector, size: Dp = 44.dp, tint: Color = AstraGold) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2417), Color(0xFF0E0C08))))
            .border(1.dp, AstraGold.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Four-bar signal indicator driven by ping. */
@Composable
fun SignalBars(pingMs: Int, modifier: Modifier = Modifier) {
    val level = when {
        pingMs < 100 -> 4
        pingMs < 200 -> 3
        pingMs < 350 -> 2
        else -> 1
    }
    val color = when (level) {
        4, 3 -> AstraGreen
        2 -> AstraAmber
        else -> AstraRed
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..4) {
            Box(
                Modifier
                    .width(4.dp)
                    .height((4 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= level) color else Color.White.copy(alpha = 0.15f))
            )
        }
    }
}

/** Gold-outlined pill used for status labels. */
@Composable
fun GoldPill(text: String, modifier: Modifier = Modifier, dot: Color? = null, textColor: Color = AstraTextPrimary) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2417), Color(0xFF0E0C08))))
            .border(1.dp, AstraGold.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}

/** Screen header: gold badge + title (start side) with optional trailing action. */
@Composable
fun ScreenHeader(title: String, icon: ImageVector, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoldBadge(icon, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = AstraTextPrimary)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}
