# giwon-assistant-api

아침 브리핑, 아이디어 정리, 일정/할 일 요약을 제공하는 개인 AI 비서 API다.
지금은 브리핑 이력과 아이디어를 DB에 저장하는 형태까지 들어간 초기 제품 단계다.

## MVP 방향
- 아침마다 하루 계획, 날씨, 뉴스 요약을 한 번에 정리
- 떠오른 아이디어나 생각을 요약 가능한 구조로 변환
- 일정과 할 일을 오늘 기준 계획으로 묶어서 보여주기

## 현재 구현 범위
- `GET /api/v1/briefings/today`
- `GET /api/v1/briefings/history`
- `GET /api/v1/briefings/schedule`
- `POST /api/v1/ideas`
- `GET /api/v1/ideas`
- `GET /api/v1/ideas/{id}`
- `PATCH /api/v1/ideas/{id}`
- `POST /api/v1/ideas/summaries`
- `GET /api/v1/plans/today`
- `GET /api/v1/copilot/today`
- `GET /api/v1/copilot/history`
- `POST /api/v1/copilot/ask`
- `GET /actuator/health`

현재 날씨는 Open-Meteo를 통해 실제 값을 받아오고,
뉴스는 Google News RSS를 통해 상위 헤드라인을 가져오며,
캘린더는 provider 구조를 먼저 만들고 설정 기반 이벤트를 기본값으로 사용한다.
아이디어 요약은 OpenAI Responses API를 붙일 수 있게 만들었고,
API 키가 없거나 실패하면 mock 응답으로 fallback 한다.
아이디어와 브리핑 이력은 JPA + Flyway 기반으로 저장된다.
자동 브리핑은 기본적으로 매일 오전 8시(Asia/Seoul) 스케줄로 동작하며, 같은 날 자동 브리핑은 중복 저장하지 않는다.

## 기술 스택
- Kotlin
- Spring Boot 3.3
- Spring Web
- Spring Validation
- Spring Actuator
- Spring Data JPA
- Flyway
- H2 / PostgreSQL
- Docker

## 로컬 실행
```bash
./gradlew bootRun
```

- 기본 프로필은 파일 기반 H2를 사용한다.
- 그래서 DB를 따로 띄우지 않아도 아이디어/브리핑 이력이 로컬 파일에 저장된다.

## Docker 실행
```bash
docker compose up -d --build
```

- Docker 실행 시에는 PostgreSQL 프로필로 올라간다.
- 기본 포트:
  - API: `8080`
  - PostgreSQL: `5436`

## CORS
- `http://localhost:4173`
- `http://127.0.0.1:4173`

`giwon-home` 프론트에서 직접 호출할 수 있도록 기본 CORS를 열어뒀다.

## API 예시
```bash
curl http://localhost:8080/api/v1/briefings/today
```

```bash
curl http://localhost:8080/api/v1/briefings/history
```

```bash
curl http://localhost:8080/api/v1/copilot/today
```

```bash
curl http://localhost:8080/api/v1/copilot/history
```

```bash
curl -X POST http://localhost:8080/api/v1/copilot/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"오늘 뭐부터 하면 좋을까?"}'
```

```bash
curl -X POST http://localhost:8080/api/v1/ideas/summaries \
  -H 'Content-Type: application/json' \
  -d '{"title":"아침 브리핑 제품화","rawText":"아침마다 날씨와 일정, 뉴스 요약을 자동으로 정리해주고 싶다."}'
```

```bash
curl -X POST http://localhost:8080/api/v1/ideas \
  -H 'Content-Type: application/json' \
  -d '{"title":"AI 비서 자동화","rawText":"아침마다 날씨와 일정, 뉴스, 할 일을 정리해주는 기능을 만들고 싶다.","tags":["assistant","automation"]}'
```

## 다음 단계
- Google Calendar 실제 연동
- Notion / News 연동
- 사용자별 브리핑 템플릿 분리
- 자동 브리핑 결과 전송 채널 추가
- 브리핑/계획/아이디어를 기반으로 한 대화형 코파일럿 고도화

## 참고
- 날씨 데이터는 Open-Meteo Forecast API를 기준으로 연동했다.
- 캘린더는 Google Calendar provider를 붙일 수 있도록 구조를 먼저 분리했다.
- OpenAI 연동은 공식 Responses API 기준으로 붙였다.
- 뉴스 연동은 Google News RSS 기반으로 붙였고, 비활성화 시 mock headline 으로 fallback 한다.
