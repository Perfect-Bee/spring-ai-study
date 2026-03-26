# Chapter 7 모델 전.후처리 어드바이저

## Advisor 소개

Advisor는 Spring 애플리케이션과 LLM 간의 상호작용을 가로채어, LLM에게 전달되는 프롬프트를 강화하거나 LLM의 응답을 변환하는 유연하고 강력한 기능

LLM과의 상호작용에서 반복적으로 사용되는 전처리 및 후처리 로직을 캡슐화하여, 재사용 가능하고 유지 관리가 용이한 AI 구성 요소를 만들 수 있음

전처리는 주로 프롬프트에 컨텍스트를 추가하는 과정으로 응답 정확도를 높이기 위해 로컬 데이터, 이전 대화 내용, 현재 상황 설명등이 추가됨

[로컬 데이터]

LLM이 학습하지 않은 데이터로, 데이터베이스, 백터 저장소에서 가져온 정보를 뜻함

LLM이 유사한 데이터로 학습한 적이 있어도 추가된 컨텍스트는 응답 생성 시 최우선적으로 반영되기 때문에 보다 정확한 답변 유도 가능

[이전 대화 내용]

LLM은 이전 대화 내용을 자체적으로 저장하지 않습니다. 과거 대화 내용을 반영한 응답을 생성하려면, 매요청마다 이전 대화 내용을 컨텍스트로 추가해서 LLM에 전달해야 합니다.

[현재 상황]

LLM은 사용자의 실시간 상황을 알 수 없기 때문에, 실시간 정보를 컨텍스트로 제공하면 보다 정확하고 적절한 응답을 생성할 수 있습니다. 예를 들어, 날씨, 온도, 위치와 같은 현재 상황에 대한 데이터를 컨텍스트를 통해 알려줄 수 있습니다.

후처리 작업은 주로 LLM의 응답을 검사하고, 애플리케이션이 요구하는 형식으로 변환하는 과정을 의미합니다. 또한, 사용자의 질문과 LLM의 응답을 함께 데이터베이스나 백터 저장소에 기록하여 이후 검색이나 문맥 유지에 활용할 수도 있습니다.

Spring AI의 Advisor는 Spring Web의 인터셉터 개념과 유사, 요청과 응답의 흐름을 가로채어 전처리/후처리를 수행합니다. 그리고 체인을 형성해서 요청 전처리와 응답 후처리를 순차적으로 실행

LLM의 요청과 응답 사이에 여러 개의 Advisor가 Chain 형태로 구성될 수 있음.

Spring AI의 Advisor 구조는 기존 Spring AOP와 같이, 공통 기능의 분리와 재사용을 가능하게 함.

예시)

- 사용자 요청을 데이터베이스에서 검색하여 프롬프트에 추가
- 요청과 응답에 대한 안정성을 필터링
- 사용자 위치, 날씨 등 외부 정보를 프롬프트에 추가

Advisor는 이러한 기능을 체인으로 엮어 순차적으로 실행, 우선순위를 설정하여 체인내에서 흐름 제어가 가능하기 때문에 복잡한 AI 파이프라인을 쉽게 구성할 수 있습니다.

## Spring AI Advisor API

Spring AI의 Advisor API는 LLM 요청과 응답 사이에서 사용할 수 있도록 설계된 핵심 인터페이스 집합

- CallAdvisor : 동기 방식으로 LLM을 호출할 때 사용됩니다.
- StreamAdvisor : 비동기 스트리밍 방식으로 LLM을 호출할 때 사용됩니다.

CallAdvisor와 StreamAdvisor는 Ordered 인터페이스를 상속하고 있어, 실행 우선순위를 지정할 수 잇습니다. 낮은 값일수록 높은 우선순위를 가지고 되며, HIGHEST_PRECEDENCE와 LOWEST_PRECEDENCE 상수를 통해 상대적인 우선순위를 쉽게 지정 가능

이 둘은 CallAdviosrChain과 SteamAdvisorChain에 의해 관리되며, 우선순위를 높은 순으로 차례대로 호출됩니다.

CallAdvisorChain은 CallAdvisor의 adviseCall() 메소드가 실행될 때 매개변수로 제공되고, StreamAdvisorChain은 StreamAdvisor의 adviseStream() 메소드가 실행될 때 매개변수로 제공됩니다.

CallAdvisorChain과 StreamAdvisorChain은 다음 Advisor를 호출하기 위해 nextCall()과 nextStream() 메소드를 각각 가지고 있습니다.

### Ordered 인터페이스

```java
public interface Ordered {

	/**
	 * Useful constant for the highest precedence value.
	 * @see java.lang.Integer#MIN_VALUE
	 */
	int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	/**
	 * Useful constant for the lowest precedence value.
	 * @see java.lang.Integer#MAX_VALUE
	 */
	int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

	/**
	 * Get the order value of this object.
	 * <p>Higher values are interpreted as lower priority. As a consequence,
	 * the object with the lowest value has the highest priority (somewhat
	 * analogous to Servlet {@code load-on-startup} values).
	 * <p>Same order values will result in arbitrary sort positions for the
	 * affected objects.
	 * @return the order value
	 * @see #HIGHEST_PRECEDENCE
	 * @see #LOWEST_PRECEDENCE
	 */
	int getOrder();

}
```

- `HIGHEST_PRECEDENCE` ≤ getOrder() < `LOWEST_PRECEDENCE`
- Advisor의 실행 순서를 결정하는 getOrder()를 통해 낮은 숫자부터 전처리 실행
- 만약 동일한 반환값을 가진 Advisor가 여러 개 있을 경우, 실행 순서 보장되지 않음

### Advisor 인터페이스

```java
public interface Advisor extends Ordered {

	/**
	 * Useful constant for the default Chat Memory precedence order. Ensures this order
	 * has lower priority (e.g. precedences) than the Spring AI internal advisors. It
	 * leaves room (1000 slots) for the user to plug in their own advisors with higher
	 * priority.
	 */
	int DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER = Ordered.HIGHEST_PRECEDENCE + 1000;

	/**
	 * Return the name of the advisor.
	 * @return the advisor name.
	 */
	String getName();

}
```

- Advisor의 고유한 이름 정보를 알 수 있는 getName() 제공

### CallAdvisor와 CallAdvisorChain 인터페이스

```java
public interface CallAdvisor extends Advisor {
	ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain);
}

public interface CallAdvisorChain extends AdvisorChain {
	/**
	 * Invokes the next {@link CallAdvisor} in the {@link CallAdvisorChain} with the given
	 * request.
	 */
	ChatClientResponse nextCall(ChatClientRequest chatClientRequest);
}
```

- CallAdvisor는 동기 호출 흐름에 개입하며, adviseCall() 메소드를 통해 ChatClientRequest를 가로채고, 적절한 시점에 체인의 다음 CallAdvisor를 실행시킵니다.
- 체인은 CallAdvisorChain으로 관리되며, nextCall() 메소드를 통해 다음 CallAdvisor를 호출

### StreamAdvisor와 StreamAdvisorChain 인터페이스

```java
public interface StreamAdvisor extends Advisor {
	Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain);
}

public interface StreamAdvisorChain extends AdvisorChain {
	Flux<ChatClientResponse> nextStream(ChatClientRequest chatClientRequest);
}
```

- StreamAdvisor는 Flux 기반의 비동기 스트리밍 호출에 개입하여, adviseStream() 메소드를 통해 개별 응답 조각을 제어할 수 있습니다.
    - 이를 통해 음성 응답, 토큰 단위 출력 등 스트리밍 시나리오에서도 세밀한 흐름 제어가 가능
- 체인은 StreamAdvisorChain 인터페이스로 관리되며, nextStream() 메소드를 통해 다음 StreamAdvisor를 호출

### ChatClientRequest와 ChatClientResponse 레코드

ChatCIientRequest는 요청(Prompt) 정보를 가지고 있고, ChatClientResponse는 응답(CallResponse) 정보를 가지고 있습니다. 두 레코드 모두, 전체 체인에 걸쳐 공유해야 할 데이터를 위해 Map<String, Object> 타입의 context를 가지고 있습니다.

```java
public record ChatClientRequest(
		Prompt prompt, Map<String, @Nullable Object> context
) {}

public record ChatClientResponse(
		@Nullable ChatResponse chatResponse, Map<String, @Nullable Object> context
) {}
```

## Advisor 구현

```java
public class AdvisorA implements CallAdvisor, StreamAdvisor {
  @Override
  public String getName() { 
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() { 
    // 순서 반환값으로 호출 순서 제어 가능
    // AdvisorB = Ordered.HIGHEST_PRECEDENCE + 2
    // AdvisorC = Ordered.HIGHEST_PRECEDENCE + 3
    // 전처리 작업은 AdvisorA -> AdvisorB -> AdvisorC
    // 후처리 작업은 AdvisorC -> AdvisorB -> AdvisorA
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    log.info("[전처리]");
    ChatClientResponse response = chain.nextCall(request);
    log.info("[후처리]");
    return response;
  }

  @Override
  public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
    log.info("[전처리]");
    Flux<ChatClientResponse> response = chain.nextStream(request); 
    return response; 
  }
}
```

- adviseCall() 메소드를 재정의해서 동기 방식으로 전처리와 후처리 작업 코드를 작성합니다. 전처리와 후처리 코드의 경계는 nextCall() 호출 코드입니다.
- adviseStream() 메소드를 재정의해서 비동기 스트림 방식으로 전처리와 후처리 작업 코드를 작성합니다. 전처리와 후처리 코드의 경계는 nextStream() 호출 코드입니다.

## Advisor 적용

Advisor를 ChatClient에 적용하는 방법은 두 가지가 있습니다. ChatClient를 생성할 때, 기본 Adviso로 추가하는 방법이 있고, 요청마다 Advisor를 추가하는 방법이 있습니다.

### 기본 Advisor로 추가하는 방법

```java
this.chatClient = chatClientBuilder
   .defaultAdvisors(Advisor ... advisor)
   .build();
```

### 요청 시 마다 Advisor를 ChatClient에 추가하는 방법

```java
chatClient.prompt()
    .advisors(Advisor ... advisor)
    ...
```

### 실제 적용 코드

```java
@Service
@Slf4j
public class AiService1 {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService1(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            new AdvisorA(),
            new AdvisorB())
        .build();
  }

  // ##### 메소드 #####
  public String advisorChain1(String question) {
    String response = chatClient.prompt()
        .advisors(new AdvisorC())
        .user(question)
        .call()
        .content();
    return response;
  }
  
  public Flux<String> advisorChain2(String question) {
    Flux<String> response = chatClient.prompt()
        .advisors(new AdvisorC())
        .user(question)
        .stream()
        .content();
    return response;
  }  
}
```

## 공유 데이터 이용

ChatClientRequest와 ChatClientResponse는 전체 Advisor Chain에서 공유해야 할 데이터를 위해 Map<String, Object> 타입의 context를 가지고 있습니다.

ChtClient가 실행될 때 Advisor와 공유해야 할 데이터가 있다면 다음 코드로 키와 값을 제공할 수 있습니다.

```java
chatClient.prompt()
   .advisors(advisorSpec -> advisorSpec.param("키", 값))
   ...
```

각 Advisor는 ChatClientRequest 또는 CahtClientResponse 레코드의 context() 메소드를 통해 공유 데이터인 Map<String, Object>를 얻고, 여기에 저장된 값을 읽고 활용할 수 있습니다.

### 실제 적용 예시

최대 응답 문자 수를 300자로 제한하는 지시문을 프롬프트에 추가하는 Advisor

```java
public class MaxCharLengthAdvisor implements CallAdvisor {
  // ##### 필드 #####
  public static final String MAX_CHAR_LENGH = "maxCharLength";
  private int maxCharLength = 300; //공통 최대 문자 수
  private int order;

  // #### 생성자 #####
  public MaxCharLengthAdvisor(int order) {
    this.order = order;
  }

  // ##### 메소드 #####
  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    // 전처리 작업: 사용자 메시지가 강화된 ChatClientRequest 얻기
    ChatClientRequest mutatedRequest = augmentPrompt(request);
    // 다음 Advisor 호출 또는 LLM으로 요청
    ChatClientResponse response = chain.nextCall(mutatedRequest);
    // 응답 반환
    return response;
  }

  // 사용자 메시지 강화
  private ChatClientRequest augmentPrompt(ChatClientRequest request) {
    // 추가할 사용자 텍스트 얻기
    String userText = this.maxCharLength + "자 이내로 답변해주세요.";    
    Integer maxCharLength = (Integer) request.context().get(MAX_CHAR_LENGH);
    if (maxCharLength != null) {
      userText = maxCharLength + "자 이내로 답변해주세요.";
    }
    String finalUserText = userText;
    
    // 사용자 메시지를 강화한 Prompt 얻기
    Prompt originalPrompt = request.prompt();
    Prompt augmentedPrompt = originalPrompt.augmentUserMessage(
        userMessage -> UserMessage.builder()
            .text(userMessage.getText() + " " + finalUserText)
            .build());

    // 수정된 ChatClientRequest 얻기
    ChatClientRequest mutatedRequest = request.mutate()
        .prompt(augmentedPrompt)
        .build();
    return mutatedRequest;
  }
}
```

advisor를 적용한 service

```java
public class AiService2 {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService2(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .defaultAdvisors(new MaxCharLengthAdvisor(Ordered.HIGHEST_PRECEDENCE))
        .build();
  }

  // ##### 메소드 #####
  public String advisorContext(String question) {
    String response = chatClient.prompt()
        .advisors(advisorSpec -> 
          advisorSpec.param(MaxCharLengthAdvisor.MAX_CHAR_LENGH, 100))
        .user(question)
        .call()
        .content();
    return response;
  } 
}

```

## 내장 Advisor

### 로깅 Advisor

SimpleLoggerAdvisor : ChatClient의 요청과 응답 내용을 로깅

### 사용자 질문 검사 Advisor

SafeGuardAdvisor : 사용자 질문에서 민감한 단어가 포함되어 있을 경우 요청을 처리하지 않고, 차단합니다.

### 대화 기억 Advisor (9장)

MessageChatMemoryAdvisor : 대화 기억을 메시지 모음으로 프롬프트에 추가

PromptChatMemoryAdvisor : 대화 기억을 프롬프트의 시스템 텍스트에 추가

VectorStoreChatMemoryAdvisor : 대화 기억을 백터 저장소에서 검색하여 프롬프트의 시스템 텍스트에 추가

### 검색 증강 생성(RAG) Advisor (10장)

QuestionAnswerAdvisor : 사용자의 질문과 관련된 내용을 벡터 저장소에서 조회하고, 결과를 사용자 메시지에 추가

RetrievalAugmentationAdvisor : 모듈식 아키텍처 기반 Advisor로 런타임 시 다양한 모듈을 결합하여 프롬프트를 강화

## 로깅 Advisor

### 로깅 레벨 제어

```java
// application.properties
logging.pattern.console=%clr(%-5level){green} %clr(%logger.%M\\(\\)){cyan}: %msg%n
logging.level.org.springframework.ai.chat.client.advisor=DEBUG
```

### 적용 예시

```java
@Service
@Slf4j
public class AiService3 {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService3(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            new MaxCharLengthAdvisor(Ordered.HIGHEST_PRECEDENCE),
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE-1)
        )
        .build();
  }

  // ##### 메소드 #####
  public String advisorLogging(String question) {
    String response = chatClient.prompt()
        .advisors(advisorSpec -> advisorSpec.param("maxCharLength", 100))
        .user(question)
        .call()
        .content();
    return response;
  } 
}

```

## 세이프가드 Advisor

프롬프트에서 개발자가 지정한 폭력, 혐오, 개인 정보, 회사 기밀 정보 등의 민감한 단어가 포함되어 있을 경우, 요청을 차단하는 기능을 제공

```java
@Service
@Slf4j
public class AiService4 {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService4(ChatClient.Builder chatClientBuilder) {
    SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(
        List.of("욕설", "계좌번호", "폭력", "폭탄"),
        "해당 질문은 민감한 콘텐츠 요청이므로 응답할 수 없습니다.",
        Ordered.HIGHEST_PRECEDENCE
    );
    
    this.chatClient = chatClientBuilder
        .defaultAdvisors(safeGuardAdvisor)
        .build();
  }

  // ##### 메소드 #####
  public String advisorSafeGuard(String question) {
    String response = chatClient.prompt()
        .user(question)
        .call()
        .content();
    return response;
  } 
}

```

- new SafeGuardAdvisor 생성자 파라미터
    - sensitiveWords는 프롬프트에서 찾을 민감한 단어 목록
    - failureResponse는 민감한 단어가 발견되었을 때 제공될 응답 텍스트
    - order는 SafeGuardAdvisor가 체인 내에서 가질 우선 순위