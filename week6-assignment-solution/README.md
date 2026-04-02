# Week 6 Assignment - AI 도서 추천 시스템

## 프로젝트 개요

AI 기반 도서 추천 시스템에 **Advisor(로깅/가드)**, **Memory(멀티턴 대화)**, **Embedding(시맨틱 검색)** 기능을 추가하는 과제입니다.

PostgreSQL(RDB)에 저장된 20권의 도서 데이터가 앱 시작 시 PGVector(VectorStore)에 자동 동기화되며, 사용자는 자연어로 도서를 검색하고 AI와 대화하며 추천을 받을 수 있습니다.

## 기술 스택

- Spring Boot 4.0.2 + Spring AI 2.0.0-M2
- PostgreSQL 17 + PGVector (Docker)
- OpenAI (gpt-4.1-nano + text-embedding-ada-002)

## 실행 방법

```bash
# 1. Docker 시작
docker compose up -d

# 2. 환경변수 설정
export OPENAI_API_KEY=your-key

# 3. 앱 실행
./gradlew bootRun

# 4. 브라우저 접속
http://localhost:8080
```

## 과제 TODO (6개)

### TODO 1 - 단순 유사도 검색 (Chapter 8)
**파일**: `BookSearchService.java` → `searchSimple()`

VectorStore의 `similaritySearch(query)` 메소드를 사용하여 자연어 쿼리로 유사한 도서를 검색합니다.

### TODO 2 - 상세 조건 유사도 검색 (Chapter 8)
**파일**: `BookSearchService.java` → `searchAdvanced()`

`SearchRequest.builder()`를 사용하여 topK, similarityThreshold, filterExpression(장르 필터)을 적용한 상세 검색을 구현합니다.

### TODO 3 - 임베딩 정보 확인 (Chapter 8)
**파일**: `BookSearchService.java` → `getEmbeddingInfo()`

`EmbeddingModel.embedForResponse()`를 사용하여 텍스트를 임베딩하고, 모델명/차원/벡터 정보를 반환합니다.

### TODO 4 - Advisor Chain 구성 (Chapter 7 + 9)
**파일**: `BookChatService.java` → 생성자

ChatClient에 3개의 Advisor를 등록합니다:
1. **SafeGuardAdvisor** - 민감 단어 차단
2. **PromptChatMemoryAdvisor** - JDBC 기반 멀티턴 대화 기억
3. **SimpleLoggerAdvisor** - 요청/응답 로깅

### TODO 5 - 멀티턴 도서 추천 채팅 (Chapter 7 + 8 + 9 통합)
**파일**: `BookChatService.java` → `chat()`

시맨틱 검색으로 관련 도서를 찾고, 대화 기억을 활용하여 일관된 AI 도서 추천 응답을 생성합니다.

### TODO 6 - 도서 등록 시 RDB + VectorStore 동시 저장 (실무 패턴)
**파일**: `BookService.java` → `registerBook()`

새 도서를 RDB에 저장하고, 동시에 VectorStore에도 임베딩하여 즉시 시맨틱 검색이 가능하도록 합니다. RDB(원본) + VectorStore(검색 인덱스) 동기화 패턴을 학습합니다.

## 아키텍처

```
[사용자 질문]
    ↓
[SafeGuardAdvisor] → 민감 단어 차단
    ↓
[PromptChatMemoryAdvisor] → 이전 대화 기억을 시스템 메시지에 추가
    ↓
[SimpleLoggerAdvisor] → 요청/응답 DEBUG 로깅
    ↓
[BookSearchService] → VectorStore에서 관련 도서 시맨틱 검색
    ↓
[LLM (gpt-4.1-nano)] → 검색된 도서 + 대화 기억 기반 추천 응답 생성
    ↓
[사용자에게 응답]
```

## 데이터 흐름

```
[Docker Init] → PostgreSQL에 20권 도서 INSERT
       ↓
[BookDataInitializer] → 앱 시작 시 RDB 전체 도서 → VectorStore 임베딩 저장
       ↓
[VectorStore (PGVector)] → 시맨틱 검색 인덱스로 활용
[ai_chat_memory 테이블] → JDBC 기반 대화 기억 영구 저장
```

## pgAdmin 접속

- URL: http://localhost:5050
- 서버 등록: Host=`pgvector-db`, DB=`book_db`, User=`app_user`, PW=`app_password`
