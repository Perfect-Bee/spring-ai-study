## Model의 종류

- Chat Model

  Text, Image To Text

- Image Model

  Text To Image

- Audio Model

  Text To Speech, Speech To Text

- Embedding Model

  Text To Vector/

💡Spring AI는 이런 여러 모델을 API로 제공하고 있기 때문에 AI 모델에 종속되어 있지 않다.
따라서 AI 모델을 변경해도 소스 코드의 변경이 최소화된다.

## Chat Model

### call() 메서드

- 매개값으로 문자열(메시지, 프롬프트)을 가지고 LLM에게 `동기` 요청을 보냄
    - LLM으로부터 완전한 응답을 받고 String 또는 ChatResponse로 반환

### StreamChatModel의 stream() 메서드

- 매개값으로 문자열(메시지, 프롬프트)을 가지고 LLM에게 `비동기` 요청을 보냄
    - LLM으로부터 텍스트 스트림 응답을 받으면 Flux<String> or Flux<ChatResponse>로 반환


💡Flux<T> 비동기적 이벤트 기반 타입
- 여러 개의 T 항목을 순차적으로 읽을 수 있는 스트림
- T 항목을 List, Stream 처럼 한번에 가져와 메모리에 올려놓고 사용하는 것과 달리, Flux는 모든 T 항목을 한꺼번에 가져오지 않고 하나씩 청크 단위로 순차적으로 가져와 사용
- 내부적으로 이벤트 루프나 비동기 스케줄러를 활용해 T 항목을 처리
    - I/O 작업이 끝날 때까지 스레드를 블로킹하지 않음
- 응답 본문의 타입은 MediaType.APPLICATION_NDJSON_VALUE(application/x-ndjson)이 되어 T 객체들이 한 줄씩 JSON으로 직렬화 되어 라인 단위로 순차적 출력됨.
- 실시간 채팅, 로그 스트리밍, 서버 이벤트 전송(SSE)등 다양한 시나리오에서 쓰임

```java
@Service
@Slf4j
public class AiService {
  // ##### 필드 #####
  @Autowired
  private ChatModel chatModel;

  // ##### 메소드 #####
  public String generateText(String question) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("사용자 질문에 대해 한국어로 답변을 해야 합니다.")
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .build();
    
    // 대화 옵션 설정
    ChatOptions chatOptions = ChatOptions.builder()
        .model("gpt-4o-mini")
        .temperature(0.3)
        .maxTokens(1000)
        .build();

    // 프롬프트 생성
    Prompt prompt = Prompt.builder()
        .messages(systemMessage, userMessage)
        .chatOptions(chatOptions)
        .build();

    // LLM에게 요청하고 응답받기
    ChatResponse chatResponse = chatModel.call(prompt);
    AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
    String answer = assistantMessage.getText();

    return answer;
  }

  public Flux<String> generateStreamText(String question) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("사용자 질문에 대해 한국어로 답변을 해야 합니다.")
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .build();

    // 대화 옵션 설정
    ChatOptions chatOptions = ChatOptions.builder()
        .model("gpt-4o")
        .temperature(0.3)
        .maxTokens(1000)
        .build();

    // 프롬프트 생성
    Prompt prompt = Prompt.builder()
        .messages(systemMessage, userMessage)
        .chatOptions(chatOptions)
        .build();

    // LLM에게 요청하고 응답받기
    Flux<ChatResponse> fluxResponse = chatModel.stream(prompt);
    Flux<String> fluxString = fluxResponse.map(chatResponse -> {
      AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
      String chunk = assistantMessage.getText();
      if (chunk == null) chunk = "";
      return chunk;
    });

    return fluxString;
  }
}
```

### Prompt

- 복수 개의 시스템 메시지, 사용자 메시지, AI 메시지 저장
- LLM 요청 시 적용할 대화 옵션(ChatOptions) 소유

💡ChatOptions
- 모델 이름
- 최대 토큰 수
- Temperature
    - 출력 다양성을 조절하는 온도 값(0.0~1.0), 값이 클수록 다양한 응답 생성
- TopK
    - 상위 K개 후보 단어를 고려한 뒤 그중에서 무작위로 선택하는 방식, K가 클수록 다양한 응답 생성
- TopP
    - 누적 확률이 P(0.0~1.0) 이하인 단어 중에서 선택하는 방식, P가 클수록 더 다양한 응답 생성
- PresencePenalty
    - 새로운 단어 사용을 장려하는 패널티 값(-2.0~2.0), 값이 클수록 다양한 응답 생성
- FrequencyPenalty
    - 동일한 단어나 구의 반복을 억제하는 패널티 값(-2.0~2.0), 값이 클수록 반복이 줄어듬
- StopSequences
    - 응답 생성을 중단할 기준이 되는 문자열 목록

### Message 인터페이스

LLM 요청 전

- SystemMessage

  LLM의 행동과 응답 스타일을 지시하는 메시지

  주로 LLM이 입력을 해석하는 방법과 답변하는 방식 지시

- UserMessage

  사용자의 질문/명령


LLM 요청 후

- AssistantMessage

  LLM 응답 메시지

  대화 기억 유지에 사용, 일관되고 맥락에 맞는 대화에 활용

- ToolResponseMessage

  도구 호출 결과를 LLM으로 다시 반환할 때 활용하는 내부 메시지


## ChatClient

대화 기억을 유지하거나, 벡터 저장소의 유사도 검색 결과를 추가하거나, 도구 호출을 위한 내부 메시지 교환 등과 같은 복잡한 데이터 흐름을 관리하는 기능이 ChatModel에 없기 때문에 이러한 복잡한 데이터 흐름을 관리하는 고급 수준의 ChatClient가 탄생

ChatClient는 어드바이저들을 체인으로 엮어 전.후처리를 할 수 있음, 어드바이저들은 순차적으로 실행하면서 프롬프트를 보강, 예를 들어 이전 대화 내용을 프롬프트에 추가하거나, RAG 시스템에서 벡터 저장소의 검색 결과를 프롬프트에 추가하는 작업이 가능

```java
@Service
@Slf4j
public class AiServiceByChatClient {
  // ##### 필드 #####
  private ChatClient chatClient;
  
  // ##### 생성자 #####
  public AiServiceByChatClient(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public String generateText(String question) {
    String answer = chatClient.prompt()
        .system("사용자 질문에 대해 한국어로 답변을 해야 합니다.")
        .user(question)
        .options(ChatOptions.builder()
            .temperature(0.3)
            .maxTokens(1000)
            .build()
        )
        .call()
        .content();
    
    return answer;
  }

  public Flux<String> generateStreamText(String question) {
    Flux<String> fluxString = chatClient.prompt()
        .system("사용자 질문에 대해 한국어로 답변을 해야 합니다.")
        .user(question)
        .options(ChatOptions.builder()
            .temperature(0.3)
            .maxTokens(1000)
            .build()
        )        
        .stream()
        .content();
  
    return fluxString;
  }
}
```