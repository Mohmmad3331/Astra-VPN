package com.astra.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.astra.vpn.ui.theme.*

/**
 * Split-tunneling (per-app VPN include/exclude) and general settings.
 * App picker list wiring is intentionally left for a follow-up pass —
 * the layout and state surface are ready to hang it off of.
 */
@Composable
fun SettingsScreen(
    splitTunnelOnlySelected: Boolean,
    onSplitTunnelToggle: (Boolean) -> Unit,
    onManageApps: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AstraBackground)
            .padding(20.dp)
    ) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium, color = AstraTextPrimary)
        Spacer(Modifier.height(20.dp))

        SettingsCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Split Tunneling", color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "فقط برنامه‌های انتخابی از تونل رد بشن",
                        color = AstraTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = splitTunnelOnlySelected,
                    onCheckedChange = onSplitTunnelToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = AstraIndigo, checkedTrackColor = AstraIndigo.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(onClick = onManageApps) {
            Column {
                Text("انتخاب برنامه‌ها", color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    "مثلاً فقط یوتیوب، اینستاگرام و اپ‌های هوش مصنوعی",
                    color = AstraTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                Text("دربارهٔ سرورها", color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    "سرورها از پروژهٔ عمومی VPN Gate (دانشگاه سوکوبا) تأمین می‌شن و توسط داوطلبان اجرا می‌شن. برای استفادهٔ روزمره (ویدیو، شبکهٔ اجتماعی، ابزارهای هوش مصنوعی) مناسبن؛ برای بانکداری یا اطلاعات حساس توصیه نمی‌شه.",
                    color = AstraTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(AstraSurfaceElevated)
        .padding(16.dp)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Box(modifier) { content() }
}
