package com.mute.shutter.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.mute.shutter.ConnectionStatus
import com.mute.shutter.MainUiState
import com.mute.shutter.R

@Composable
fun PairingGuideScreen(
    modifier: Modifier = Modifier,
    state: MainUiState,
    onPairPortChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onConnectPortChange: (String) -> Unit,
    onWlanIpChange: (String) -> Unit,
    onApplyMute: () -> Unit,
    onToggleAdvanced: () -> Unit,
    onResetPairing: () -> Unit,
    onOpenUsageAccess: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        StatusCard(state)

        if (state.needsUsageAccess) {
            OutlinedButton(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) {
                Text("사용 통계 허용")
            }
        }

        if (state.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        if (!state.isPaired) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.pairPort,
                        onValueChange = onPairPortChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("페어링 포트") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.pin,
                        onValueChange = onPinChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("6자리 PIN") },
                        singleLine = true,
                    )
                }
            }
            Button(
                onClick = onApplyMute,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
            ) {
                Text("처음 설정하기", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onApplyMute,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
            ) {
                Text(
                    text = if (state.status == ConnectionStatus.Muted) "다시 적용" else "셔터음 끄기",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        TextButton(onClick = onToggleAdvanced, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.showAdvanced) "고급 설정 닫기" else "고급 설정")
        }

        AnimatedVisibility(visible = state.showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.wlanIp,
                    onValueChange = onWlanIpChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("IP (수동)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.connectPort,
                    onValueChange = onConnectPortChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("연결 포트 (수동)") },
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = onResetPairing,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    Text("처음부터 다시 설정")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when (state.status) {
                    ConnectionStatus.Muted -> "셔터음 꺼짐"
                    ConnectionStatus.PairedNotConnected -> "페어링됨"
                    ConnectionStatus.Connected -> "연결됨"
                    ConnectionStatus.NotPaired -> "설정 필요"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (state.status) {
                    ConnectionStatus.Muted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
