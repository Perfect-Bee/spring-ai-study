# Week 6 Practice Project — Spring AI 임베딩·벡터 저장소·대화 기억·Advisor

Spring AI의 임베딩, 벡터 저장소, 대화 기억(Chat Memory), Advisor 기능을 익히는 실습 프로젝트입니다.

## 기술 스택

- Java 17, Spring Boot 4.0.2, Spring AI 2.0.0-M2
- OpenAI (`gpt-4.1-nano`, `text-embedding-ada-002`)
- PostgreSQL + pgvector (벡터 저장소)
- pgAdmin (DB 관리 UI)
- Thymeleaf 웹 UI

---

## 이론

| 주제 | 핵심 개념 |
|------|-----------|
| 임베딩 | EmbeddingModel 추상화, 텍스트 → 벡터 변환, 차원·모델 메타데이터 |
| 벡터 저장소 | VectorStore 추상화, PgVectorStore, Document 저장·유사도 검색·필터 삭제 |
| Chat Memory (InMemory) | InMemoryChatMemoryRepository, MessageChatMemoryAdvisor, 휘발성 대화 기억 |
| Chat Memory (JDBC) | JdbcChatMemoryRepository, PromptChatMemoryAdvisor, PostgreSQL 영구 저장 |
| Chat Memory (VectorStore) | VectorStoreChatMemoryAdvisor, 의미 기반 유사 대화 검색 |
| Advisor 패턴 | CallAdvisor/StreamAdvisor 인터페이스, Advisor Chain, Context 공유 |
| 내장 Advisor | SimpleLoggerAdvisor (로깅), SafeGuardAdvisor (민감어 필터링) |

---

## 실습 내용

### 1. 임베딩 & 벡터 저장소 (`EmbeddingService`)
- 텍스트 임베딩: `embeddingModel.embedForResponse()` → 벡터 차원·모델명·벡터 샘플 조회
- Document 저장: `vectorStore.add()` 로 메타데이터 포함 Document 벡터 저장소에 저장
- 유사도 검색 (단순): `vectorStore.similaritySearch(query)` 기본 검색
- 유사도 검색 (상세): `SearchRequest.builder()` 로 topK·similarityThreshold·filterExpression 설정
- Document 삭제: `vectorStore.delete(filterExpression)` 필터 기반 삭제

### 2. 대화 기억 — InMemory (`InMemoryChatService`)
- `InMemoryChatMemoryRepository` 를 명시적으로 생성하여 휘발성 보장
- `MessageWindowChatMemory` 로 최대 메시지 수(20) 제한
- `MessageChatMemoryAdvisor`: 대화 기억을 메시지 모음(UserMessage + AssistantMessage)으로 프롬프트에 추가
- `conversationId` 로 사용자/세션별 대화 분리

### 3. 대화 기억 — JDBC (`JdbcChatService`)
- `JdbcChatMemoryRepository` + PostgreSQL 로 서버 재시작 후에도 대화 기억 유지
- `PromptChatMemoryAdvisor`: 대화 기억을 텍스트 형태로 시스템 메시지에 포함
- `MessageWindowChatMemory` 로 최대 메시지 수(100) 제한, 오래된 메시지 자동 제거

### 4. 대화 기억 — VectorStore (`VectorStoreChatService`)
- `VectorStoreChatMemoryAdvisor` + 별도 `PgVectorStore` (chat_memory_vector_store 테이블)
- 현재 대화와 의미적으로 유사한 이전 대화를 검색하여 프롬프트에 추가 (topK=5)
- 대화가 많을 때 관련 기억만 선택적으로 활용하는 방식

### 5. Advisor 패턴 (`AdvisorService`)
- `SimpleLoggerAdvisor`: 요청/응답 DEBUG 로깅
- `SafeGuardAdvisor`: 민감한 단어(욕설, 계좌번호, 폭력 등) 포함 시 차단
- `MaxCharLengthAdvisor` (커스텀): 시스템 메시지 + 사용자 메시지에 글자 수 제한 지시문 추가, Context 파라미터로 값 전달
- `RequestTimingAdvisor` (커스텀): 전처리/후처리에서 LLM 요청 소요 시간 측정
- Advisor Chain: 여러 Advisor를 order 기반으로 체이닝하여 순차 적용
- 스트리밍 지원: `CallAdvisor` + `StreamAdvisor` 동시 구현으로 동기/비동기 양쪽 지원

---

## 실행 방법

```bash
# 1. PostgreSQL + pgvector 실행
docker-compose up -d

# 2. 애플리케이션 실행
export OPENAI_API_KEY=sk-your-key-here
./gradlew bootRun
```

실행 후 http://localhost:8080 에서 웹 UI로 모든 기능을 테스트할 수 있습니다.
pgAdmin은 http://localhost:5050 에서 접속할 수 있습니다 (admin@example.com / admin1234).

---
