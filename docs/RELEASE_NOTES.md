# Play Store 출시 노트

> Play Console → 출시 → 출시 노트에 복사·붙여넣기

---

## 무료 앱 — 카메라 셔터 무음 (무료) v1.1.0

```
v1.1.0 (안드11 지원 및 안정성 개선)

• Android 11 이상 지원 (기존 Android 13 → 확대)
• 앱 실행 시 광고 추가 (배너·전면·앱 오픈)
• 포그라운드 서비스 호출 개선 (모든 안드로이드 버전 호환)
• 광고 계정 보안 강화 (테스트/릴리스 광고 단위 분리)
• 방해 금지 모드 권한 개선
• 삼성 갤럭시 S25·S26 및 기존 기종 지원
```

---

## 유료 앱 — 카메라 셔터 무음

```
v1.0.9 (이전 버전 — 무료 앱 출시로 업데이트 종료)

• 이전에 제공되던 유료 버전입니다.
• 동일 기능이 이제 무료 버전에서 광고와 함께 제공됩니다.
```

---

## AAB 빌드

```powershell
.\gradlew.bat bundlePaidRelease bundleFreeRelease
```

- 유료: `app\build\outputs\bundle\paidRelease\app-paid-release.aab`
- 무료: `app\build\outputs\bundle\freeRelease\app-free-release.aab`
