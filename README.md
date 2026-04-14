# giwon-assistant-api

아침 브리핑, 아이디어 정리, 일정/할 일 요약을 제공하는 개인 AI 비서 API다.
브리핑 이력과 아이디어를 DB에 저장하고, Notion과 양방향 연동하는 V2 단계다.

## 현재 구현 범위

### 브리핑
- `GET /api/v1/briefings/today` — 날씨/일정/뉴스/할일 기반 하루 브리핑 생성
- `GET /api/v1/briefings/history` — 최근 브리핑 이력 조회
- `GET /api/v1/briefings/schedule` — 자동 브리핑 스케줄 상태 확인

### 아이디어
- `POST /api/v1/ideas` — 아이디어 생성 (AI 요약 포함)
- `GET /api/v1/ideas` — 아이디어 목록 조회
- `GET /api/v1/ideas/{id}` — 아이디어 상세 조회
- `PATCH /api/v1/ideas/{id}` — 아이디어 수정
- `POST /api/v1/ideas/summaries` — 아이디어 요약만 생성

### 코파일럿
- `GET /api/v1/copilot/today` — 오늘 코파일럿 요약
- `GET /api/v1/copilot/history` — 코파일럿 대화 이력
- `POST /api/v1/copilot/ask` — 코파일럿 질문

### 기타
- `GET /api/v1/plans/today` — 오늘 계획 조회
- `GET /actuator/health`

## 외부 연동 현황

| 연동 | 방식 | 활성화 조건 |
|------|------|------------|
| 날씨 | Open-Meteo API | 기본 활성화 |
| 뉴스 | Google News RSS | `news-enabled=true` |
| 캘린더 | Google Calendar OAuth2 | `calendar-enabled=true` + credentials |
| 브리핑 요약 | Claude API (Anthropic) | `claude-enabled=true` + `ANTHROPIC_API_KEY` |
| 아이디어 요약 | OpenAI Responses API | `openai-enabled=true` + `OPENAI_API_KEY` |
| 코파일럿 | Gemini 우선 / OpenAI fallback | `gemini-enabled=true` |
| Notion | REST API | `ASSISTANT_NOTION_ENABLED=true` + token + DB ID |

외부 연동이 실패하거나 비활성화된 경우 mock 데이터로 fallback하며, 응답의 `mock: true` 필드로 구분할 수 있다.

## 기술 스택
- Kotlin / Spring Boot 3.3
- Spring Web (RestClient)
- Spring Data JPA / Flyway
- H2 (로컬) / PostgreSQL (Docker)
- Docker

## 로컬 실행

```bash
./gradlew bootRun
```

기본 프로필은 파일 기반 H2를 사용한다. DB를 따로 띄우지 않아도 데이터가 로컬 파일에 저장된다.

**Claude 브리핑 요약 활성화:**
```bash
ANTHROPIC_API_KEY=sk-ant-... \
ASSISTANT_INTEGRATIONS_CLAUDE_ENABLED=true \
./gradlew bootRun
```

**Google Calendar 연동 활성화:**
```bash
ASSISTANT_INTEGRATIONS_CALENDAR_ENABLED=true \
ASSISTANT_GOOGLE_CLIENT_ID=... \
ASSISTANT_GOOGLE_CLIENT_SECRET=... \
ASSISTANT_GOOGLE_REFRESH_TOKEN=... \
./gradlew bootRun
```

**Notion 연동 활성화:**
```bash
ASSISTANT_NOTION_ENABLED=true \
ASSISTANT_NOTION_TOKEN=ntn_... \
./gradlew bootRun
```

**Gemini 코파일럿 활성화:**
```bash
GEMINI_API_KEY=... \
ASSISTANT_INTEGRATIONS_GEMINI_ENABLED=true \
./gradlew bootRun
```

## Docker 실행

```bash
docker compose up -d --build
```

- PostgreSQL 프로필로 올라간다.
- API: `8080`, PostgreSQL: `5436`
- 환경변수는 `.env` 파일 또는 셸 환경변수로 전달한다.
- 런타임 이미지에는 `wget`을 포함해 이후 compose/컨테이너 healthcheck에 바로 대응할 수 있게 맞춰뒀다.

## API 예시

```bash
# 오늘 브리핑
curl http://localhost:8080/api/v1/briefings/today

# 아이디어 생성
curl -X POST http://localhost:8080/api/v1/ideas \
  -H 'Content-Type: application/json' \
  -d '{"title":"AI 비서 음성 입력","rawText":"아침 브리핑을 음성으로 받으면 더 편할 것 같다.","tags":["AI","UX"]}'

# 코파일럿 질문
curl -X POST http://localhost:8080/api/v1/copilot/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"오늘 뭐부터 하면 좋을까?"}'
```

## CORS
- `http://localhost:4173` / `http://127.0.0.1:4173`

`giwon-home` 프론트에서 직접 호출할 수 있도록 기본 CORS를 열어뒀다.

## 다음 단계
- tasks 하드코딩 제거 — 실제 action DB 연동
- Conversation Copilot 고도화 — 대화 컨텍스트 유지
- 주간 리뷰 자동화 — 브리핑 로그 기반 리포트 생성
- 배포 환경 정리

## 참고
- 날씨: Open-Meteo Forecast API
- 뉴스: Google News RSS
- 캘린더: Google Calendar API v3 (OAuth2 refresh token)
- 브리핑 요약: Anthropic Messages API (`claude-sonnet-4-5`)
- 아이디어 요약: OpenAI Responses API
- 코파일럿: Gemini `generateContent` API
- Notion: Notion REST API v1
