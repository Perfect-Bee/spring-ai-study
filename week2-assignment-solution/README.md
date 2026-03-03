# Week 2 과제 답안 - AI 기반 도서 추천 시스템

> **이 프로젝트는 `week2-assignment-project`의 모범 답안입니다.**
> 수강생에게 과제 제출 후 참고용으로 제공됩니다.

## 구현 요약

| TODO | 메서드 | 핵심 기법 |
|------|--------|-----------|
| TODO 1 | `recommendBooks()` | ChatClient Fluent API (`prompt → user → call → content`) |
| TODO 2 | `analyzeBook()` | ClassPathResource + PromptTemplate 변수 치환 |
| TODO 3 | `getStructuredRecommendations()` | `entity()` + `ParameterizedTypeReference` |
| TODO 4 | `classifyBookZeroShot()` | 제로-샷 프롬프트 (예시 없이 카테고리와 지시문만 제공) |
| TODO 5 | `analyzeWithStepBack()` | 스텝-백 프롬프트 (상위 개념 탐색 → 적용 → 최종 답변) |

## 핵심 구현 포인트

### TODO 1: ChatClient 기본 사용
`String.format()`으로 프롬프트를 구성한 뒤, `chatClient.prompt().user(prompt).call().content()` 체인으로 텍스트 응답을 획득합니다.

### TODO 2: PromptTemplate 활용
`ClassPathResource`로 `prompts/book-analysis.st` 파일을 로드하고, `PromptTemplate.create(Map.of(...))`로 변수를 치환합니다. 템플릿 안의 `{title}`, `{author}` 플레이스홀더가 실제 값으로 대체됩니다.

### TODO 3: 구조화된 출력
`call()` 이후 `content()` 대신 `entity(new ParameterizedTypeReference<List<BookRecommendation>>() {})`를 사용하여 JSON 응답을 Java 객체 리스트로 자동 변환합니다.

### TODO 4: 제로-샷 프롬프트
분류 카테고리 목록을 명시하고, 응답 형식을 구체적으로 지정합니다. 예시를 제공하지 않으므로 AI가 자체 판단으로 분류합니다.

### TODO 5: 스텝-백 프롬프트
3단계 구조(상위 개념 탐색 → 적용 → 최종 답변)로 프롬프트를 설계합니다. 바로 답하지 않고 배경 지식을 먼저 정리한 뒤 답변하므로 더 깊이 있는 분석이 가능합니다.

## 실행 방법

### 방법 1: OpenAI (기본)

```bash
export OPENAI_API_KEY=sk-...
./gradlew bootRun
```

### 방법 2: Ollama (로컬 LLM)

```bash
ollama pull llama3.2
./gradlew bootRun --args='--spring.profiles.active=ollama'
```

### 웹 UI 접근
- URL: `http://localhost:8080`
- 5개의 탭에서 기능 테스트
