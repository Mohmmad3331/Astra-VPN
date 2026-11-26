package com.astra.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.astra.vpn.ui.theme.*

enum class AstraTab(val label: String, val icon: ImageVector) {
    HOME("خانه", Icons.Filled.Shield),
    SERVERS("سرورها", Icons.Filled.Public),
    SETTINGS("تنظیمات", Icons.Filled.Settings)
}

@Composable
fun AstraBottomBar(selected: AstraTab, onSelect: (AstraTab) -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AstraTab.values().forEach { tab ->
                val isSel = tab == selected
                val tint by animateColorAsState(if (isSel) AstraGold else AstraTextSecondary, label = "tabTint")
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .let {
                            if (isSel) it.background(
                                Brush.verticalGradient(listOf(AstraGold.copy(alpha = 0.28f), AstraGold.copy(alpha = 0.06f)))
                            ) else it.background(Color.Transparent)
                        }
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(tab.label, color = tint, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
