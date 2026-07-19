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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onOpenDndAccess: () -> Unit = {},
    onShowDebugLog: () -> Unit = {},
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

        state.statusMessage?.let { message ->
            val isError = !state.isLoading && state.status != ConnectionStatus.Muted
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        if (state.status != ConnectionStatus.Muted) {
            SetupGuideCard()
        }

        if (state.status == ConnectionStatus.Muted) {
            SetupCompleteCard(needsUsageAccess = state.needsUsageAccess)
        }

        if (state.needsUsageAccess) {
            OutlinedButton(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) {
                Text("사용 통계 허용")
            }
        }

        if (state.needsDndAccess) {
            Text(
                text = "벨소리 무음 전환에는 「방해 금지 모드 접근」 권한이 필요합니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = onOpenDndAccess, modifier = Modifier.fillMaxWidth()) {
                Text("방해 금지 모드 접근 허용")
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

        if (state.status != ConnectionStatus.Muted) {
            if (!state.isPaired) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "① 무선 디버깅 → 「페어링 코드로 기기 페어링」 팝업의 값",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "② 무선 디버깅 메인 화면의 「IP 주소 및 포트」 값 (페어링 포트와 다름, 재부팅 시마다 바뀜)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = state.connectPort,
                        onValueChange = onConnectPortChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("연결 포트") },
                        singleLine = true,
                    )
                }
            }

            Button(
                onClick = onApplyMute,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
            ) {
                Text(
                    text = if (state.isPaired) "다시 연결" else "처음 설정하기",
                    style = MaterialTheme.typography.titleMedium,
                )
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
                if (state.status == ConnectionStatus.Muted) {
                    Text(
                        text = "연결 포트: 무선 디버깅 메인 화면 「IP 주소 및 포트」 값 (재부팅 시마다 바뀜)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = state.connectPort,
                        onValueChange = onConnectPortChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("연결 포트") },
                        singleLine = true,
                    )
                }
                OutlinedButton(
                    onClick = onResetPairing,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    Text("처음부터 다시 설정")
                }
            }
        }

        TextButton(
            onClick = onShowDebugLog,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🐛 디버그 로그 보기")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_guide_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            listOf(
                R.string.setup_guide_step1,
                R.string.setup_guide_step2,
                R.string.setup_guide_step3,
                R.string.setup_guide_step4,
            ).forEach { stepRes ->
                Text(
                    text = stringResource(stepRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    val (titleRes, descRes, containerColor, contentColor) = when (state.status) {
        ConnectionStatus.Muted -> StatusStyle(
            titleRes = R.string.status_muted_title,
            descRes = R.string.status_muted_desc,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        ConnectionStatus.PairedNotConnected -> StatusStyle(
            titleRes = R.string.status_paired_title,
            descRes = R.string.status_paired_desc,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ConnectionStatus.Connected -> StatusStyle(
            titleRes = R.string.status_connected_title,
            descRes = R.string.status_connected_desc,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ConnectionStatus.NotPaired -> StatusStyle(
            titleRes = R.string.status_not_paired_title,
            descRes = R.string.status_not_paired_desc,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SetupCompleteCard(needsUsageAccess: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_complete_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.setup_complete_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (needsUsageAccess) {
                Text(
                    text = stringResource(R.string.status_muted_usage_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class StatusStyle(
    val titleRes: Int,
    val descRes: Int,
    val containerColor: Color,
    val contentColor: Color,
)
