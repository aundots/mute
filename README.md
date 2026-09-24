# Mute — Samsung 셔터음 0 (Android 16)

**Android 16 (API 36)** 전용. PC adb와 동일한 3단계를 폰 안에서 실행합니다.

```bash
adb pair IP:페어링포트 PIN
adb connect IP:연결포트
adb shell settings put system csc_pref_camera_forced_shuttersound_key 0
```

## 요구 사항

- **Android 16** (API 36) — arm64
- Samsung Galaxy 한국향 (셔터 CSC 설정)
- **설정 → 시스템 → 개발자 옵션 → 무선 디버깅** ON
- Wi‑Fi 연결 (인터넷 불필요, AP 연결만 필요)
- 앱 **주변 기기** 권한 허용 (adb connect용)

## 빌드

```bash
cd mute
.\gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## 사용법

### 최초 1회

1. 무선 디버깅 ON → **「이 네트워크에서 항상 허용」** 체크 권장
2. **주변 기기** 권한 허용 (앱 최초 adb 시 요청)
3. 「페어링 코드로 기기 페어링」→ IP, 페어링 포트, PIN 입력 → **페어링**
4. 무선 디버깅 메인 화면의 **연결 포트** 입력 → **연결 및 무음 적용**

> 페어링 포트 ≠ 연결 포트

### 이후

- 앱 실행 시 저장된 IP·포트로 자동 `adb connect` + 설정 `0` 재적용
- 재부팅 후에는 연결 포트가 바뀔 수 있음 → 새 포트만 입력

## Android 16 참고

- 무선 디버깅 메뉴: **설정 → 시스템 → 개발자 옵션 → 무선 디버깅**
- 신뢰 Wi‑Fi 네트워크에서는 무선 디버깅이 자동 유지되는 빌드도 있음 (기기·빌드별)
- `targetSdk 36` — 로컬 네트워크 접근에 `NEARBY_WIFI_DEVICES` 선언

## 기술

- compileSdk / targetSdk / minSdk **36**
- LADB 방식 번들 `libadb.so` (nativeLibraryDir에서 실행)
