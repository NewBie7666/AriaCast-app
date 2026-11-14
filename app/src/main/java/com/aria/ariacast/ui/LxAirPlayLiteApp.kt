package com.aria.ariacast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aria.ariacast.AudioCastService
import com.aria.ariacast.CastState
import com.aria.ariacast.DiscoveryState
import com.aria.ariacast.pipeline.SourceKind
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Private-purpose UI: LX Music -> Android internal playback capture -> AirPlay 2.
 * No microphone input, screen video, plug-ins, HTTP remote, or DLNA UI.
 */
@Composable
fun LxAirPlayLiteApp(controller: AppController) {
    val servers by controller.servers.collectAsState()
    val discoveryState by controller.discoveryState.collectAsState()
    val service by controller.service.collectAsState()
    val fallbackState = remember { MutableStateFlow(CastState.OFF) }
    val fallbackError = remember { MutableStateFlow<String?>(null) }
    val fallbackPairing = remember { MutableStateFlow<AudioCastService.PairingRequest?>(null) }
    val state by (service?.state ?: fallbackState).collectAsState()
    val error by (service?.lastError ?: fallbackError).collectAsState()
    val pairing by (service?.pairingPinRequest ?: fallbackPairing).collectAsState()
    val targets = servers.filter { it.platform == "AirPlay2" }
    var selectedKey by rememberSaveable { mutableStateOf("") }
    val chosen = targets.firstOrNull { "${it.host}:${it.port}" == selectedKey } ?: targets.firstOrNull()
    var pinText by rememberSaveable { mutableStateOf("") }

    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("LX AirPlay Lite", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("仅将洛雪音乐的播放音频发送到 AirPlay 2 音箱。")
                Text("需要安装官方洛雪音乐，手机与音箱处于同一局域网。", style = MaterialTheme.typography.bodySmall)
                Text("当前状态：${when (state) {
                    CastState.OFF -> "未连接"
                    CastState.DISCOVERING -> "正在搜索"
                    CastState.CONNECTING -> "正在连接"
                    CastState.CASTING -> "正在投放"
                    CastState.ERROR -> "连接失败"
                }}", style = MaterialTheme.typography.titleMedium)

                if (error != null && state == CastState.ERROR) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AirPlay 2 音箱", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = controller::refreshDiscovery) { Text("刷新") }
                }
                if (targets.isEmpty()) {
                    Text(if (discoveryState == DiscoveryState.SCANNING) "正在搜索局域网中的 AirPlay 2 音箱…" else "未发现 AirPlay 2 接收设备。请确认 Home 350 已联网且手机位于同一局域网。")
                }
                targets.forEach { target ->
                    val key = "${target.host}:${target.port}"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RadioButton(selected = chosen == target, onClick = { selectedKey = key })
                        TextButton(onClick = { selectedKey = key }) {
                            Text("${target.name} · ${target.host}")
                        }
                    }
                }

                if (state == CastState.CASTING || state == CastState.CONNECTING) {
                    Button(onClick = controller::stopCasting, modifier = Modifier.fillMaxWidth()) {
                        Text("停止投放")
                    }
                } else {
                    Button(
                        onClick = {
                            chosen?.let { controller.castTo(listOf(it), SourceKind.SYSTEM_CAPTURE) }
                        },
                        enabled = chosen != null && service != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("开始 AirPlay 2 投放") }
                }
                Text("v0.1.1 · AirPlay 2 自动发现", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "隐私：应用只允许捕获洛雪音乐的媒体音频。Android 仍会要求麦克风权限及投屏授权；本版本不使用物理麦克风。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("投放结束后请点击「停止投放」，释放音频捕获与网络连接。", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (pairing != null) {
        AlertDialog(
            onDismissRequest = { service?.resetPairingPinRequest() },
            title = { Text("音箱配对验证") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请提供 ${pairing?.name ?: "AirPlay 2 音箱"} 显示的 PIN 或密码。")
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { pinText = it },
                        label = { Text("PIN / 密码") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pairing?.let { service?.submitPairingPin(it.host, pinText) }
                    pinText = ""
                }, enabled = pinText.isNotBlank()) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = {
                    service?.resetPairingPinRequest()
                    pinText = ""
                }) { Text("取消") }
            },
        )
    }
}
