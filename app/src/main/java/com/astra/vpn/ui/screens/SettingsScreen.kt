package com.astra.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.astra.vpn.ui.components.*
import com.astra.vpn.ui.theme.*

/**
 * Split-tunneling (per-app VPN include/exclude) and general settings.
 * App picker wiring is unchanged: the state surface is ready for it.
 */
@Composable
fun SettingsScreen(
    mirrorHost: String,
    onMirrorHostChange: (String) -> Unit,
    onApplyMirror: () -> Unit,
    splitTunnelOnlySelected: Boolean,
    onSplitTunnelToggle: (Boolean) -> Unit,
    onManageApps: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("تنظیمات", Icons.Filled.Settings) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    GoldBadge(Icons.AutoMirrored.Filled.ArrowBack, size = 40.dp)
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GoldBadge(Icons.Filled.Layers)
                    Spacer(Modifier.width(14.dp))
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AstraBlack,
                            checkedTrackColor = AstraGold,
                            uncheckedThumbColor = AstraTextSecondary,
                            uncheckedTrackColor = Color_TrackOff
                        )
                    )
                }
            }

            GlassCard(Modifier.fillMaxWidth(), onClick = onManageApps) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GoldBadge(Icons.Filled.Apps)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("انتخاب برنامه‌ها", color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "مثلاً فقط یوتیوب، اینستاگرام و اپ‌های هوش مصنوعی",
                            color = AstraTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Filled.ChevronLeft, null, tint = AstraGold)
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text("آدرس آینه (Mirror)", color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "اگه لیست سرورها بالا نیومد (vpngate.net فیلته)، آدرس یه آینه رو اینجا بذار.",
                        color = AstraTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    val clipboard = LocalClipboardManager.current
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = mirrorHost,
                                onValueChange = onMirrorHostChange,
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("http://host:port") },
                                shape = GlassShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AstraGold,
                                    unfocusedBorderColor = AstraGold.copy(alpha = 0.45f),
                                    focusedTextColor = AstraTextPrimary,
                                    unfocusedTextColor = AstraTextPrimary,
                                    cursorColor = AstraGold,
                                    focusedPlaceholderColor = AstraTextSecondary,
                                    unfocusedPlaceholderColor = AstraTextSecondary
                                )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let(onMirrorHostChange)
                        }) {
                            Icon(Icons.Filled.ContentPaste, "چسباندن", tint = AstraGold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onApplyMirror) {
                        Icon(Icons.Filled.Refresh, null, tint = AstraGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ذخیره و بروزرسانی لیست", color = AstraGold)
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = AstraGold, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("دربارهٔ سرورها", color = AstraTextPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "سرورها از پروژهٔ عمومی VPN Gate (دانشگاه سوکوبا) تأمین می‌شن و توسط داوطلبان اجرا می‌شن. برای استفادهٔ روزمره (ویدیو، شبکهٔ اجتماعی، ابزارهای هوش مصنوعی) مناسبن؛ برای بانکداری یا اطلاعات حساس توصیه نمی‌شه.",
                        color = AstraTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private val Color_TrackOff = androidx.compose.ui.graphics.Color(0xFF2A2A2A)
