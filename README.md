# RoutineHub – 마이프로틴 가격 추적 & 예측 서비스

마이프로틴(IMPACT WHEY 2×2.5 kg)의 **실시간 가격**을 수집·저장하고, ARIMA/ARX 모델로 **단기 가격을 예측**하여 리액트 대시보드로 시각화하는 **풀스택 애플리케이션**입니다.
<img width="1897" height="850" alt="image" src="https://github.com/user-attachments/assets/4b650776-d8e2-43b9-be88-10b109c02148" />
<img width="1912" height="903" alt="image" src="https://github.com/user-attachments/assets/4fc9ee33-7448-4864-8a47-8f11303a3fb1" />

---

## ✨ 주요 기능

| 영역                           | 설명                                                                                            |
| ---------------------------- | --------------------------------------------------------------------------------------------- |
| **크롤러 (Selenium)**           | 매일 **오전 7시(KST)** 장바구니에 접속 → 할인코드 적용 → 원가·할인가·할인율 크롤링                                         |
| **REST API (Spring Boot 3)** | `/api/price/**` 엔드포인트로 가격 히스토리·베스트타임·다중 예측 제공                                                 |
| **가격 예측**                    | 휴일 더미 변수를 포함한 ARX(1) 모델 → **내일·7일·30일·N일 후·다음 달 최저가** 예측                                      |
| **대시보드 (React 18)**          | `PriceDashboard.jsx` : 히스토리 & 필터, 30일 예측 버튼<br>`PriceAnalysis.jsx` : 내일/최저/커스텀 예측·가격 하락 확률 표시 |
| **스타일**                      | Tailwind 유틸리티 + `App.css`, 차트는 **Recharts** 사용                                                |
|**1RM 계산**|여러 1RM 공식을 통한 계산과 추천|

---

## 🏗️ 아키텍처

```
┌──────────────┐   스케줄 @07:00 KST   ┌────────────┐
│  Selenium    │ ────────────────────▶ │ Myprotein  │
│  Crawler     │                      │  WebSite   │
└──────┬───────┘                      └────────────┘
       │  PriceEntry(JPA)                    ▲
       ▼                                      │
┌──────────────┐ ForecastEntry(JPA)   ┌───────┴─────────┐
│ Spring Boot  │ ◀────────────────── │   MySQL(RDS)   │
│   REST API   │                    └─────────────────┘
└──────┬───────┘  JSON                     ▲
       │                                   │ Axios
       ▼                                   │
┌──────────────┐                           │
│   React      │ ◀─────────────────────────┘
│ Dashboard    │
└──────────────┘
```

---

## 🖥️ 기술 스택

* **백엔드**: Java 17, Spring Boot 3.5.x, Spring Data JPA, Selenium 4.31+, Apache Commons Math, MySQL 8
* **프론트엔드**: React 18, Vite/CRA, Recharts 2.x, Axios
* **배포**: Ubuntu 22.04 EC2, Amazon RDS(MySQL), systemd 서비스(`routine-hub-*.jar`)

---

## ⚙️ 로컬 실행 가이드

### 1) 환경 준비

| 도구                     | 권장 버전                 |
| ---------------------- | --------------------- |
| Java 17+               | `sdk install java 17` |
| Node 18+               | `nvm install 18`      |
| MySQL 8                | 로컬 또는 RDS             |
| Chrome / Chromium 115+ | 헤드리스 실행 바이너리          |
| ChromeDriver 동일 버전     | PATH 등록               |

```bash
# 프로젝트 클론
$ git clone https://github.com/your‑org/routine‑hub.git
$ cd routine‑hub
```

### 2) 데이터베이스

```sql
CREATE DATABASE routinehub
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

`application.properties`의 `spring.datasource.*` / `hibernate.jdbc.time_zone=Asia/Seoul` 값을 확인하세요.

### 3) 백엔드 실행

```bash
./mvnw spring-boot:run -Duser.timezone=Asia/Seoul
# or
./mvnw clean package -DskipTests
java -jar target/routine-hub-*.jar
```

첫 실행 시 스케줄러가 **다음 날 07:00** 작업을 예약합니다.

### 4) 프론트엔드 실행

```bash
cd front‑routine‑hub
npm install
npm start  # http://localhost:3000
```

`axios.defaults.baseURL` 값을 백엔드 주소로 맞춰주세요.

---

## 🔌 API 요약

| Method & URL                                | Query                                           | 응답                      |
| ------------------------------------------- | ----------------------------------------------- | ----------------------- |
| `GET /api/price/myprotein/history`          | `code`, `period=daily\|weekly\|monthly\|yearly` | `PriceEntry[]`          |
| `GET /api/price/myprotein/best-time`        | `code`                                          | `BestTimeDto`           |
| `GET /api/price/myprotein/arima-forecast`   | `code`                                          | `ArmaForecastDto`       |
| `GET /api/price/myprotein/forecast/weekly`  | `code`                                          | 7일치 `MultiForecastDto`  |
| `GET /api/price/myprotein/forecast/monthly` | `code`                                          | 30일치 `MultiForecastDto` |
| `GET /api/price/myprotein/forecast/offset`  | `code`, `offset=N`                              | `ForecastPoint`         |

*모든 타임스탬프는 **KST(UTC+9)** 오프셋 포함 ISO‑8601 형식.*

---

## 🕑 스케줄러 & 타임존

* **크롤링** : `@Scheduled(cron = "0 0 7 * * *", zone="Asia/Seoul")`
* **예측 스냅샷(1‑7일)** : `@Scheduled(cron = "0 5 7 * * *", zone="Asia/Seoul")`
* EC2/DB 시간대 모두 **Asia/Seoul**

---

## 🛠 문제 해결 — 리눅스 환경 Headless Chrome + Selenium

### 1. `session not created: probably user data directory is already in use`

**원인**  
Snap Chromium 은 기본 프로필(`~/.config/chromium`)을 두 인스턴스 이상 열지 못함 → 잠금 충돌.

**해결**

```java
// 실행마다 고유 프로필 사용
Path tmp = Files.createTempDirectory("cprof-");
opts.addArguments("--user-data-dir=" + tmp);

try (ChromeDriver driver = new ChromeDriver(opts)) {
    … // 크롤링 작업
} finally {
    FileSystemUtils.deleteRecursively(tmp); // 잠금 파일 제거
}
````

---

### 2. `DevToolsActivePort file doesn’t exist`

**원인**
ChromeDriver 버전 ≠ 크롬(Chromium) 버전 → 브라우저가 즉시 크래시.

**해결**

```java
// A) 버전을 “점까지” 맞춤 (chromium --version 결과 그대로)
WebDriverManager.chromedriver()
        .driverVersion("138.0.7204.168")
        .setup();

// B) Snap 대신 google‑chrome‑stable(.deb) 설치
//    Selenium Manager / WebDriverManager 가 알아서 드라이버 매칭
```

---

### 3. `no chrome binary at …`

**원인**
ChromeDriver 가 래퍼 스크립트만 보고 실제 ELF 바이너리를 못 찾음,
또는 프로세스가 보는 파일시스템 내에 바이너리가 없음.

**해결**

```java
String CHROME = "/opt/google/chrome/chrome"; // 실제 ELF 파일

// 드라이버에게 경로를 명시
System.setProperty("webdriver.chrome.binary", CHROME);

ChromeOptions opts = new ChromeOptions();
opts.setBinary(CHROME);

```

---

### 4. `browserPath(String)` 메서드가 없음

**원인**
WebDriverManager 하위 버전(5.3 이전)이 캐시에 남아 있음.

**해결**

> 버전과 무관하게 항상 동작
> `System.setProperty("webdriver.chrome.binary", "/opt/google/chrome/chrome")`
> 한 줄을 추가


---

#### 최종 작동 스니펫 (가장 안정 조합)

```java
// 0. 임시 프로필 (잠금 충돌 방지)
Path tmp = Files.createTempDirectory("cprof-");

// 1. 드라이버 ↔ 크롬 빌드 ‘점까지’ 일치
WebDriverManager.chromedriver()
        .driverVersion("138.0.7204.168")        // google-chrome --version 그대로
        .setup();

// 2. 브라우저 ELF 경로 명시
System.setProperty("webdriver.chrome.binary",
        "/opt/google/chrome/chrome");

ChromeOptions opts = new ChromeOptions();
opts.setBinary("/opt/google/chrome/chrome");
opts.addArguments(
        "--headless",
        "--no-sandbox",
        "--disable-dev-shm-usage",
        "--disable-setuid-sandbox",
        "--disable-gpu",
        "--window-size=1920,1080",
        "--remote-allow-origins=*",
        "--user-data-dir=" + tmp
);

ChromeDriver driver = new ChromeDriver(opts);
try {
    // …크롤링 작업…
} finally {
    driver.quit();
    FileSystemUtils.deleteRecursively(tmp);   // 임시 프로필 정리
}
```

> **핵심 교훈** 
>
> 1. **각 실행마다 독립 프로필** → Snap Chromium 잠금 회피
> 2. **브라우저 ELF 경로와 드라이버 버전을 명시** → “no chrome binary”·“DevToolsActivePort” 종결
> 3. 부족한 라이브러리·캐시·심볼릭 체인·버전 캐싱 이슈는 단계별로 확인·해결.


---

## 📂 디렉터리 구조

```
backend/
  ├─ controller/
  ├─ model/           # JPA 엔티티 & DTO
  ├─ repository/
  └─ service/
front‑routine‑hub/
  ├─ src/
  │   ├─ PriceDashboard.jsx
  │   ├─ PriceAnalysis.jsx
  │   └─ App.css
  └─ package.json
```

---

## 📜 라이선스

MIT License © 2025 RoutineHub 팀
