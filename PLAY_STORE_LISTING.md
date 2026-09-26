# Google Play Store 등록 정보 — 카메라 셔터 무음 (유료)

> Play Console 복사·붙여넣기용 — **paid** flavor (`com.mute.shutter`)  
> **전체 문구(개발자 옵션 따라하기 포함):** `docs/STORE_LISTING_COPY.md` ①번

---

## 1. 기본 정보

| 항목 | 값 |
|------|-----|
| **앱 이름** | 카메라 셔터 무음 |
| **패키지명** | `com.mute.shutter` |
| **가격** | **₩5,000 (유료)** — 첫 게시 전 설정 |
| **광고** | **없음** |
| **버전** | 1.0.0 (versionCode: 1) |
| **카테고리** | 도구 |
| **AAB** | `app\build\outputs\bundle\paidRelease\app-paid-release.aab` |

---

## 2. 스토어 문구

### 앱 이름
```
카메라 셔터 무음
```

### 간단한 설명 (80자)
```
삼성 갤럭시 카메라 셔터음 끄기. 개발자 옵션·무선 디버깅 1회만. PC 불필요, 광고 없음.
```

### 자세한 설명
→ **`docs/STORE_LISTING_COPY.md`** 의 **① 유료 앱 → 자세한 설명** 전체를 복사해 붙여넣기

---

## 3. 그래픽 에셋

| 항목 | 파일 |
|------|------|
| 앱 아이콘 512×512 | `store-assets/icon-512.png` (없으면 launcher PNG 리사이즈) |
| 그래픽 이미지 1024×500 | `store-assets/feature-graphic.png` |
| 스크린샷 | `docs/STORE_LISTING_COPY.md` ③번 촬영 목록 참고 |

---

## 4. 앱 콘텐츠 / 데이터 보안

| 항목 | 답변 |
|------|------|
| 광고 | **아니오** |
| Usage Access | **예** (카메라 감지, 로컬만) |
| FGS Special Use | **예** (카메라 무음 감시) |
| 데이터 수집 | **아니오** |
| 광고 ID | **아니오** |

---

## 5. 출시 체크리스트

- [ ] Play Console 가격 **₩5,000** (게시 전)
- [ ] `.\gradlew.bat bundlePaidRelease`
- [ ] 개인정보처리방침 URL (`docs/privacy-policy.md`)
- [ ] 스크린샷 2장 이상, 512 아이콘, 1024×500 그래픽
- [ ] Usage Stats 선언
- [ ] 내부 테스트 → 프로덕션
