# Chapter 9 대화 기억

## 대화 기억과 기억 저장소

대규모 언어 모델은 기본적으로 상태를 저장하지 않기 때문에, 이전 대화 내용을 기억하거나 그에 기반한 응답을 생성할 수 없습니다. 이러한 한계를 보완하기 위해 Spring AI는 대화 기억(Chat Memory) 기능을 제공합니다.

### 대화 기억 (Chat Memory)

현재 세션 LLM과 대화할 때 맥락 유지하기 위해 사용하는 메시지들(UserMessage + AssistantMessage)입니다. 세션이 종료되면 없어지거나, 영구 저장할 수도 있습니다.

### 대화 기록 (Chat History)

현재 세션뿐만 아니라, 과거 세션에서 주고받은 모든 메시지들(UserMessage + AssistantMessage)을 말합니다. 과거의 대화 기억들이 꾸준히 저장된 것을 대화 기록이라고 보면 됩니다.

Spring AI가 제공하는 대화 기억은 현재 대화를 이어가기 위한 문맥을 관리하도록 설계되었기 때문에 현재 대화와 관련된 메시지만을 목적으로 합니다. 따라서 전체 대화 기록을 관리하는 최적의 솔루션이 아닙니다. 전체 대화 기록을 유지해야 한다면, JPA를 이용하는 방식을 고려해야합니다.

대화 기억을 위해 기억 유형과 기억 저장소로 기능을 분리합니다. 기억 유형은 몇 개의 메시지를 저장할지, 어떤 기간 동안 지정할지, 저체 메시지 양을 얼마로 할지를 결정하고, 기억 저장소는 단지 메시지를 저장하고 조회하는 일만 하게 됩니다.

어떤 정보를 기억할지, 그리고 언제 기억을 삭제할지는 ChatMemory 이터페이스를 구현한 클래스에 따라 달라집니다.

```java
public interface ChatMemory {
	void add(String conversationId, List<Message> messages);
	List<Message> get(String conversationId);
	void clear(String conversationId);
}
```

- conversationId는 사용자 ID입니다. 로그인한 사용자 아이디를 사용해도 좋고, 웹 환경이라면 서버에서 생성되는 세션 ID를 사용해도 좋습니다.
- add() 메소드는 사용자 ID와 함께 대화 기억을 저장합니다.
- get() 메소드는 사용자 ID로 저장된 대화 기억을 검색해서 가져옵니다.
- clear()는 사용자 ID로 저장된 대화 기억을 삭제합니다.

ChatMemory의 기본 구현체는 MessageWindowChatMemory입니다. 이 클래스는 지정된 메시지 최대 개수(메시지 윈도우)까지 메시지를 유지합니다. 메시지 수가 메시지 윈도우를 초과하면 오래된 메시지부터 제거합니다. 기본 크기는 20개입니다.

메시지 윈도우를 변경하고 싶다면 다음과 같이 MessageWindowChatMemory를 명시적으로 빌드할 때, maxMessages()를 통해 설정하면 됩니다.

```java
ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10).build();
```

ChatMemoryRepository 인터페이스를 통해 다양한 저장소에 대화 기억을 저장할 수 있습니다.

- InMemoryChatMemoryRepository : 컴퓨터 하드웨어 메모리에 저장합니다.
- JdbcChatMemoryRepository : 관계형 데이터베이스를 저장합니다.
- CassandraChatMemoryRepository : Apache Cassandra를 이용해서 시계열로 저장합니다.

기본적으로 MessageWindowChatMemory를 이용해서 ChatMemory 빈을 자동 생성하고 대화 기억 저장소는 InMemoryChatMemoryRepository를 사용합니다.

## 대화 기억을 위한 Advisor

ChatMemory가 대화 기억을 제공하면, 이것을 프롬프트에 포함해야 합니다. 이 역할을 수행하는 적임자는 Advisor입니다.

전처리 작업으로 ChatMemory로부터 받은 대화 기억을 시스템 텍스트 또는 메시지 묶음으로 프롬프트에 추가하고 LLM으로부터 응답이 오면, 후처리 작업으로 사용자의 질문(UserMessage)과 LLM의 응답(AssistantMessage)을 CHatMemory를 이용해서 대화 기억 저장소에 저장합니다.

### MessageChatMemoryAdvisor

ChatMemory에서 받은 대화 기억을 사용자 메시지와 AI 메시지들로 생성합니다. 그리고 이 메시지들을 프롬프트에  추가합니다.

```java
public AiService(
    ChatMemory chatMemory, 
    ChatClient.Builder chatClientBuilder) {   
    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            MessageChatMemoryAdvisor.builder(chatMemory).build()
        )
        .build();
}
```

### PromptChatMemoryAdvisor

ChatMemory로부터 받은 대화 기억을 텍스트 형태로 시스템 메시지에 포함시킵니다.

```java
public AiService(
    ChatMemory chatMemory, 
    ChatClient.Builder chatClientBuilder) {   
    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            PromptChatMemoryAdvisor.builder(chatMemory).build()
        )
        .build();
}
```

## Im-Memory 대화 기억

이전 대화를 기억하고 이를 바탕으로 자연스럽고 일관성 있는 응답을 생성할 수 있도록, 메모리를 대화 기억 저장소로 사용하는 방법

```java
@Service
@Slf4j
public class AiService {
  // ##### 필드 ##### 
  private ChatClient chatClient;
  
  // ##### 생성자 #####
  public AiService(
      ChatMemory chatMemory, 
      ChatClient.Builder chatClientBuilder) {   
      this.chatClient = chatClientBuilder
          .defaultAdvisors(
              MessageChatMemoryAdvisor.builder(chatMemory).build(),
              new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE-1)
          )
          .build();
  }
  
  
  // ##### 메소드 #####
  public String chat(String userText, String conversationId) {
    String answer = chatClient.prompt()
      .user(userText)
      .advisors(advisorSpec -> advisorSpec.param(
        ChatMemory.CONVERSATION_ID, conversationId
      ))
      .call()
      .content();
    return answer;
  }
}
```

## VectorStore 대화 기억

대화 기억을 벡터 저장소에 저장하면, 현재 대화와 유사한 이전 대화 기록을 검색하여 활용할 수 있다는 장점이 있습니다. 이 방식은 대화의 양이 많고, 그중에서도 현재 대화와 관련된 기억만 선택적으로 활용하고자 할 때 효과적입니다. 다만, 텍스트 임베딩과 유사성 검색 과정을 거치므로 다른 방식에 비해 응답 속도가 다소 느릴 수 있습니다.

벡터 저장소를 사용할 때는 별도의 ChatMemory 구현체를 사용하지 않고, VectorStoreChatMemoryAdvisor만으로 대화 기억을 관리할 수 있습니다. 이 Advisor는 대화 기억을 벡터 저장소에 저장하고, 현재 대화와 유사한 이전 대화를 검색해서, 프롬프트에 추가하는 모든 작업을 자체적으로 처리합니다.

```java
@Service
@Slf4j
public class AiService {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService(
      JdbcTemplate jdbcTemplate,
      EmbeddingModel embeddingModel,
      ChatClient.Builder chatClientBuilder) {

    VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .initializeSchema(false)
        .schemaName("public")
        .vectorTableName("chat_memory_vector_store")
        .build();

    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            VectorStoreChatMemoryAdvisor.builder(vectorStore).build(),
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
          )
        .build();

  }

  public String chat(String userText, String conversationId) {
    String answer = chatClient.prompt()
        .user(userText)
        .advisors(advisorSpec -> advisorSpec.param(
            ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
    return answer;
  }
}

```

- VectorStore 빌드할 때, initializeSchema() 설정 false로 해서 자동 생성 방지하고 대신에 schemaName()과 vectorTableName()으로 스키마와 테이블 이름 지정
- VectorStoreChatMemoryAdvisor 적용
- 대화 ID를 VectorStoreChatMemoryAdvisor에서 사용할 수 있도록 Advisor 공유 데이터에 ChatMemory.CONVERSATION_ID를 키로, conversationId를 값으로 저장합니다.
- VectorStoreChatMemoryAdvisor는 대화 ID를 대화 기억 저장소에서 검색 조건으로 사용하기도 하고, 대화 기억을 저장할 때도 사용합니다.

## RDBMS 대화 기억

관계형 데이터베이스에 대화 기억을 저장하려면, JdbcChatMemoryRepository를 사용합니다. 이 대화 기억 저장소는 대화 기억을 영구 저장해야 하는 애플리케이션에 적합합니다.

의존성 추가

```groovy
implementation 'org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc'
```

스키마 초기화 기능을 사용하고 싶을 때 설정

```yaml
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
```

예제 코드

```java
@Service
@Slf4j
public class AiService {
  // ##### 필드 ##### 
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService(
      JdbcChatMemoryRepository chatMemoryRepository,
      ChatClient.Builder chatClientBuilder) {    
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(chatMemoryRepository)
        .maxMessages(100)
        .build();

    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            PromptChatMemoryAdvisor.builder(chatMemory).build(),
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE-1)
        )
        .build();
  }    

  public String chat(String userText, String conversationId) {
    String answer = chatClient.prompt()
      .user(userText)
      .advisors(advisorSpec -> advisorSpec.param(
        ChatMemory.CONVERSATION_ID, conversationId
      ))
      .call()
      .content();
    return answer;
  }
}

```

- ChatMemory를 빌드할 때 chatMemoryRepository()로 JdbcChatMemoryRepository를 제공하고, 대화 기억으로 유지할 최대 메시지 수를 20개로 제한
- PromptChatMemoryAdvisor를 빌드할 때 ChatMemory를 제공
- Advisor 공유 데이터에 conversationId를 값으로 저장함으로써 대화 ID를 대화 기억 저장소에서 검색 조건으로 사용하기도 하고,  대화 기억을 저장할 때도 사용합니다.

## Cassandra 대화 기억

Apache Cassandra는 오픈 소스 NoSQL 분산 데이터베이스입니다. 기억 저장소로서 Cassandra의 장점은 대화 기억을 시계열로 저장하기 때문에 TTL 설정으로 간단하게 오래된 대화 기억을 자동으로 삭제시킬 수 있습니다.

책에 작성되어있는 자세한 세팅 내용은 최종 프로젝트에서 필요한 경우 요청해 주시면 제공해드리겠습니다.