# Play Store 출시 노트 — v1.0.2 (versionCode 3)

> Play Console → 출시 → 출시 노트에 복사·붙여넣기

---

## 유료 앱 — 카메라 셔터 무음

```
v1.0.2

• 16KB 메모리 페이지 크기 지원 (Google Play 요구사항)
• 처음 설정 방법 안내 추가
• 알림 뱃지·상단 알림 표시 최소화
• 삼성 갤럭시 S25·S26 셔터음 무음 지원
```

---

## 무료 앱 — 카메라 셔터 무음 (무료)

```
v1.0.2

• 16KB 메모리 페이지 크기 지원 (Google Play 요구사항)
• 처음 설정 방법 안내 추가
• 알림 뱃지·상단 알림 표시 최소화
• 삼성 갤럭시 S25·S26 셔터음 무음 지원
```

---

## AAB 빌드

```powershell
.\gradlew.bat bundlePaidRelease bundleFreeRelease
```

- 유료: `app\build\outputs\bundle\paidRelease\app-paid-release.aab`
- 무료: `app\build\outputs\bundle\freeRelease\app-free-release.aab`
