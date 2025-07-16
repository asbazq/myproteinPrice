# myproteinPrice
# RoutineHub – 마이프로틴 가격 추적 & 예측 서비스

마이프로틴(IMPACT WHEY 2×2.5 kg)의 **실시간 가격**을 수집·저장하고, ARIMA/ARX 모델로 **단기 가격을 예측**하여 리액트 대시보드로 시각화하는 **풀스택 애플리케이션**입니다.

---

## ✨ 주요 기능

| 영역                           | 설명                                                                                            |
| ---------------------------- | --------------------------------------------------------------------------------------------- |
| **크롤러 (Selenium)**           | 매일 **오전 7시(KST)** 장바구니에 접속 → 할인코드 적용 → 원가·할인가·할인율 크롤링                                         |
| **REST API (Spring Boot 3)** | `/api/price/**` 엔드포인트로 가격 히스토리·베스트타임·다중 예측 제공                                                 |
| **가격 예측**                    | 휴일 더미 변수를 포함한 ARX(1) 모델 → **내일·7일·30일·N일 후·다음 달 최저가** 예측                                      |
| **대시보드 (React 18)**          | `PriceDashboard.jsx` : 히스토리 & 필터, 30일 예측 버튼<br>`PriceAnalysis.jsx` : 내일/최저/커스텀 예측·가격 하락 확률 표시 |
| **스타일**                      | Tailwind 유틸리티 + `App.css`, 차트는 **Recharts** 사용                                                |

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

# 크롬 환경 변수 (예시)
export WEB_DRIVER_CHROME_BIN=/usr/bin/chromium-browser
export WEB_DRIVER_CHROME_DRIVER=/usr/bin/chromedriver
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

## 🩹 문제 해결 FAQ

| 증상                                                                  | 해결 방법                                                              |
| ------------------------------------------------------------------- | ------------------------------------------------------------------ |
| `SessionNotCreatedException: user data directory is already in use` | 크론마다 **새 ChromeOptions** + `--user-data-dir` 임시 폴더 사용 (프로젝트 기본 구현) |
| `Timeout waiting for promo-code-input`                              | 마이프로틴 페이지 구조 변경 ⇒ `scrapPrice()` 의 CSS 선택자 업데이트                    |
| 그래프 오른쪽 잘림                                                          | `.chart-wrapper { padding-right:… }` 또는 `margin={{ right: X }}` 조정 |

---

## 📂 디렉터리 구조

```
backend/
  ├─ config/          # ChromeOptions 빈
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
