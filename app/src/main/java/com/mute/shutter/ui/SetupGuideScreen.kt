package com.mute.shutter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ① 페어링용(호박색) / ② 연결용(청록색) — 스토어 가이드 이미지와 동일한 색 규칙 */
private val Pairing = Color(0xFFF0AC45)
private val PairingSoft = Color(0xFF3E2F13)
private val Connect = Color(0xFF34DCD0)
private val ConnectSoft = Color(0xFF0E3230)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("설정 가이드") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "포트 두 개만 알면 됩니다",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "폰 화면에 보이는 숫자를 앱에 그대로 옮겨 적으면 끝입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PortCard(
                accent = Pairing,
                accentSoft = PairingSoft,
                badge = "① 페어링 포트 + PIN",
                title = "최초 1회만",
                desc = "한 번 페어링하면 재부팅해도 유지됩니다.",
                where = "「페어링 코드로 기기 페어링」 팝업에서 확인",
            )
            PortCard(
                accent = Connect,
                accentSoft = ConnectSoft,
                badge = "② 연결 포트",
                title = "재부팅하면 새 번호",
                desc = "소리가 다시 나면 이 번호만 바꿔 넣으면 됩니다.",
                where = "무선 디버깅 메인 화면 「IP 주소 및 포트」",
            )

            SectionTitle("숫자는 여기에 있습니다")
            WirelessDebuggingMockup()

            SectionTitle("따라 하기")
            StepRow(1, null, "무선 디버깅 켜기",
                "설정 → 휴대전화 정보 → 소프트웨어 정보 → 「빌드 번호」 7번 탭 → 개발자 옵션에서 「무선 디버깅」 켜기")
            StepRow(2, null, "분할화면으로 나란히 두기",
                "최근 앱 목록에서 무선 디버깅을 위쪽에 고정하고 아래쪽에 이 앱을 띄우면, 숫자를 보면서 바로 입력할 수 있습니다.")
            StepRow(3, Pairing, "페어링 포트 + PIN 입력 (최초 1회)",
                "「페어링 코드로 기기 페어링」을 눌러 팝업을 열고, 6자리 PIN과 포트 번호를 입력합니다.")
            StepRow(4, Connect, "연결 포트 입력",
                "메인 화면 「IP 주소 및 포트」의 콜론(:) 뒤 숫자를 입력합니다. 한 번 성공하면 저장되므로 다음부터는 비워둬도 됩니다.")
            StepRow(5, null, "「처음 설정하기」 누르기",
                "셔터음이 꺼지면 완료입니다.")

            SectionTitle("권한 두 가지")
            PermRow("사용 통계 접근", "카메라 앱이 켜지는 순간을 감지해 자동으로 무음 처리합니다.")
            PermRow("방해 금지 모드 접근", "벨소리를 무음 모드로 바꿉니다. 허용하지 않으면 벨소리 모드는 그대로 남습니다.")

            NoteBox(
                accent = Pairing,
                boldPart = "재부팅 후 소리가 다시 나면",
                rest = " — 페어링은 유지됩니다. ② 연결 포트만 새 숫자로 바꿔 넣으세요.",
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun PortCard(
    accent: Color,
    accentSoft: Color,
    badge: String,
    title: String,
    desc: String,
    where: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, accent, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .background(accentSoft, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(badge, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        Text(
            where,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 무선 디버깅 화면에서 두 숫자가 각각 어디 있는지 보여주는 모형 */
@Composable
private fun WirelessDebuggingMockup() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("무선 디버깅", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Connect, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "IP 주소 및 포트",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                buildAnnotatedString {
                    append("192.168.0.12:")
                    withStyle(SpanStyle(color = Connect, fontWeight = FontWeight.Bold)) {
                        append("39281")
                    }
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
            )
            Box(
                modifier = Modifier
                    .background(ConnectSoft, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("② 연결 포트 · 콜론(:) 뒤 숫자", color = Connect, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Pairing, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("페어링 코드로 기기 페어링", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            MockKeyValue("Wi-Fi 페어링 코드", "483210")
            MockKeyValue("포트", "41837")
            Box(
                modifier = Modifier
                    .background(PairingSoft, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("① 페어링 포트 + PIN · 팝업 안", color = Pairing, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "페어링 값은 팝업을 열어야 나타납니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MockKeyValue(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Pairing,
        )
    }
}

@Composable
private fun StepRow(number: Int, accent: Color?, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(26.dp)
                .background(
                    accent?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermRow(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteBox(accent: Color, boldPart: String, rest: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(72.dp)
                .background(accent),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(boldPart) }
                append(rest)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
        )
    }
}
