# Context

## purpose
- 아침 브리핑, 아이디어 저장/요약, 코파일럿 질문 API 제공
- `giwon-home`의 AI 비서 화면 데이터 소스로 사용

## key features
- briefing: `today`, `history`, `schedule`
- ideas: 생성/조회/수정/요약
- copilot: 오늘 추천, 히스토리, 질문 응답
- integrations: Open-Meteo, Google News RSS, Gemini/OpenAI(선택)

## runtime notes
- local 기본 포트: `8080`
- health: `/actuator/health`
- default db: H2 file
- docker profile: PostgreSQL
- external LLM 실패 시 rule-based fallback 응답 제공

