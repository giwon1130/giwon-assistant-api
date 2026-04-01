# giwon-assistant-api

아침 브리핑, 아이디어 정리, 일정/할 일 요약을 제공하는 개인 AI 비서 API다.

## MVP 방향
- 아침마다 하루 계획, 날씨, 뉴스 요약을 한 번에 정리
- 떠오른 아이디어나 생각을 요약 가능한 구조로 변환
- 일정과 할 일을 오늘 기준 계획으로 묶어서 보여주기

## 현재 구현 범위
- `GET /api/v1/briefings/today`
- `POST /api/v1/ideas`
- `GET /api/v1/ideas`
- `GET /api/v1/ideas/{id}`
- `PATCH /api/v1/ideas/{id}`
- `POST /api/v1/ideas/summaries`
- `GET /api/v1/plans/today`
- `GET /actuator/health`

현재는 외부 연동 전 단계라 mock 기반 응답을 먼저 제공한다.
현재 날씨는 Open-Meteo를 통해 실제 값을 받아오고,
캘린더는 provider 구조를 먼저 만들고 설정 기반 이벤트를 기본값으로 사용한다.
아이디어 요약은 OpenAI Responses API를 붙일 수 있게 만들었고,
API 키가 없거나 실패하면 mock 응답으로 fallback 한다.

## 기술 스택
- Kotlin
- Spring Boot 3.3
- Spring Web
- Spring Validation
- Spring Actuator
- Docker

## 로컬 실행
```bash
./gradlew bootRun
```

## API 예시
```bash
curl http://localhost:8080/api/v1/briefings/today
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
- 아이디어/브리핑 영구 저장
- 사용자별 브리핑 템플릿 분리
- 자동 실행 스케줄러 추가

## 참고
- 날씨 데이터는 Open-Meteo Forecast API를 기준으로 연동했다.
- 캘린더는 Google Calendar provider를 붙일 수 있도록 구조를 먼저 분리했다.
- OpenAI 연동은 공식 Responses API 기준으로 붙였다.
