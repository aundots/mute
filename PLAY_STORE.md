# Mute — Google Play 출시 가이드 (유료 + 무료 2앱)

이 프로젝트는 **Gradle product flavor**로 Play Store 앱 **2개**를 빌드합니다.

| Flavor | 패키지 | 앱 이름 | 가격 | 광고 |
|--------|--------|---------|------|------|
| **paid** | `com.mute.shutter` | 카메라 셔터 무음 | ₩5,000 (유료) | 없음 |
| **free** | `com.mute.shutter.free` | 카메라 셔터 무음 (무료) | 무료 | AdMob |

---

## 1. 서명 키 만들기 (최초 1회)

```powershell
cd C:\Users\User\Desktop\mute
mkdir release
keytool -genkeypair -v -storetype PKCS12 -keystore release\upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

`keystore.properties.example`를 복사해 `keystore.properties`를 만들고 비밀번호를 입력합니다.

```powershell
copy keystore.properties.example keystore.properties
```

> **두 앱 모두 같은 서명 키**로 빌드해도 됩니다 (패키지명이 다르므로 별도 Play Console 등록).

---

## 2. Play Store용 AAB 빌드

### 유료 앱 (paid)

```powershell
.\gradlew.bat bundlePaidRelease
```

출력: `app\build\outputs\bundle\paidRelease\app-paid-release.aab`

### 무료+광고 앱 (free)

```powershell
.\gradlew.bat bundleFreeRelease
```

출력: `app\build\outputs\bundle\freeRelease\app-free-release.aab`

### 디버그 APK (개발·테스트)

```powershell
.\gradlew.bat assemblePaidDebug assembleFreeDebug
```

출력:
- `app\build\outputs\apk\paid\debug\app-paid-debug.apk`
- `app\build\outputs\apk\free\debug\app-free-debug.apk`

---

## 3. Play Console — 앱 2개 등록

Play Console에서 **별도 앱**으로 각각 등록해야 합니다.

| | 유료 (paid) | 무료 (free) |
|---|-------------|-------------|
| **패키지** | `com.mute.shutter` | `com.mute.shutter.free` |
| **가격** | ₩5,000 (첫 게시 **전** 설정) | 무료 |
| **광고** | 없음 | AdMob 포함 |
| **등록 문서** | `PLAY_STORE_LISTING.md` | `PLAY_STORE_LISTING_FREE.md` |
| **개인정보처리방침** | `docs/privacy-policy.md` | `docs/privacy-policy-free.md` |

### AdMob (무료 앱만)

1. [AdMob 콘솔](https://apps.admob.com/) → **앱 추가**
2. 패키지명 **`com.mute.shutter.free`** 로 등록 (유료 앱과 별도)
3. 배너·전면 광고 단위 생성
4. `app/src/free/res/values/strings.xml`의 테스트 ID를 실제 ID로 교체:
   - `admob_app_id`
   - `admob_banner_unit_id`
   - `admob_interstitial_unit_id`

---

## 4. 공통 체크리스트

- [ ] keystore + `keystore.properties` 설정
- [ ] 유료: Play Console 가격 **₩5,000** (게시 전)
- [ ] 무료: AdMob 앱 등록 + 실제 광고 ID 반영
- [ ] 각 앱별 개인정보처리방침 URL
- [ ] 스크린샷, 512×512 아이콘
- [ ] Usage Stats 특수 권한 선언
- [ ] 내부 테스트 → 프로덕션

---

## 5. 버전 올릴 때

`app/build.gradle.kts`의 `defaultConfig`:
- `versionCode` — 매 업로드마다 +1 (두 앱 동일하게 유지 권장)
- `versionName` — 사용자에게 보이는 버전 (예: 1.0.1)

---

## 6. 주의사항

- `upload-keystore.jks`와 `keystore.properties`는 **절대 분실·유출 금지**
- 유료 앱은 **처음부터 유료**로 게시 (무료 → 유료 전환 불가)
- 무료 앱은 **데이터 보안**에서 광고 ID 수집 **예**로 선언
- 한국/일본 등 일부 국가는 셔터음 무음 관련 법규가 있을 수 있음
