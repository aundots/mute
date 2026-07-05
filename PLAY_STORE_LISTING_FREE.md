# Google Play Store 등록 정보 — 카메라 셔터 무음 (무료)

> Play Console 복사·붙여넣기용 — **free** flavor (`com.mute.shutter.free`)  
> **전체 문구(개발자 옵션 따라하기 포함):** `docs/STORE_LISTING_COPY.md` ②번

---

## 1. 기본 정보

| 항목 | 값 |
|------|-----|
| **앱 이름** | 카메라 셔터 무음 (무료) |
| **패키지명** | `com.mute.shutter.free` |
| **가격** | **무료** |
| **광고** | **AdMob** (배너 + 전면) |
| **버전** | 1.0.0 (versionCode: 1) |
| **카테고리** | 도구 |
| **AAB** | `app\build\outputs\bundle\freeRelease\app-free-release.aab` |

---

## 2. AdMob (출시 전)

AdMob에서 **패키지 `com.mute.shutter.free`** 로 별도 앱 등록 후 `app/src/free/res/values/strings.xml`의 테스트 ID 교체.

---

## 3. 스토어 문구

### 앱 이름
```
카메라 셔터 무음 (무료)
```

### 간단한 설명 (80자)
```
삼성 갤럭시 카메라 셔터음 끄기. 개발자 옵션·무선 디버깅 1회만. PC 불필요, 무료(광고).
```

### 자세한 설명
→ **`docs/STORE_LISTING_COPY.md`** 의 **② 무료 앱 → 자세한 설명** 전체를 복사해 붙여넣기

---

## 4. 그래픽 에셋

유료 앱과 동일 아이콘·그래픽 사용 가능. 스크린샷은 무료 앱 UI(하단 배너)로 촬영 권장.

---

## 5. 앱 콘텐츠 / 데이터 보안

| 항목 | 답변 |
|------|------|
| 광고 | **예** (AdMob) |
| Usage Access | **예** |
| FGS Special Use | **예** |
| 데이터 수집 | **예** (AdMob 광고 ID) |
| 광고 ID | **예** |
| 제3자 | **예** (Google AdMob) |

개인정보처리방침: `docs/privacy-policy-free.md`

---

## 6. 출시 체크리스트

- [ ] AdMob 앱 등록 + 실제 광고 ID
- [ ] `.\gradlew.bat bundleFreeRelease`
- [ ] 개인정보처리방침 URL
- [ ] 데이터 보안 → 광고 ID **예**
- [ ] 스크린샷, 512 아이콘, 1024×500 그래픽
