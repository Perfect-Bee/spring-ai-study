## 프롬프트란?

AI 모델, 특히 대규모 언어 모델에게 사용자가 원하는 작업올 구체척으로 지시하거나 질문의 형태로 요구사항을 전달하는 일종의 명령문

프름프트는 모델에게 “어떤 상황에서”, “무엇을”, “어떤 형식으로” 응답해야 하는지를 알려주는 중요한 역할

## Spring AI의 프롬프트

Spring AI는 Prompt 클래스로 표현

프롬프트는 정적 텍스트일 수도 있지만, 데이터가 바인딩될 자리 표시자를 가지고 있는 동적 테스트인 경우도 있기 때문에 Spring에서는 PromptTemplate을 제공

### PromptTemplate

LLM에 전달할 프롬프트를 자리 표시자가 있는 텍스트 템플릿 형태로 정의하고 데이터 바인딩을 통해 동적으로 프롬프트를 완성하는 역할

```java
PromptTemplate promptTemplate = PromptTemplate.builder().template("{topic}에 대해 농답 {num}개 를 목록으로 출력해 줘.").build();

Prompt prompt = promptTemplate.create(Map.of("topic":"AI","num":3));
```

- SystemMessage는 SystemPromptTemplate
- AssistantMessage는 AssistantPromptTemplate

```java
@Slf4j
@Service
public class AiServicePromptTemplate {
    private ChatClient chatClient;

    private PromptTemplate systemTemplate = SystemPromptTemplate.builder()
            .template("""
                    답변을 생성할 때 HTML과 CSS를 사용해서 파란 글자로 출력하세요.
                    <span> 태그 안에 들어갈 내용만 출력하세요.
                    """)
            .build();

    private PromptTemplate userTemplate = PromptTemplate.builder()
            .template("""
                    다음 한국어 문장을 {language}로 번역해 주세요.
                    문장: {statement}
                    """)
            .build();

    public AiServicePromptTemplate(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    public Flux<String> promptTemplate1(String language, String statement) {
        Prompt prompt = userTemplate.create(
                Map.of("language", language, "statement", statement)
        );
        return chatClient
                .prompt(prompt)
                .stream()
                .content();
    }
    
    public Flux<String> promptTemplate2(String language, String statement) {
        return chatClient.prompt()
                .messages(
                        systemTemplate.createMessage(),
                        userTemplate.createMessage(Map.of("language", language, "statement", statement))
                )
                .stream()
                .content();
    }
    
    public Flux<String> promptTemplate3(String language, String statement) {
        return chatClient.prompt()
                .system(systemTemplate.render())
                .user(userTemplate.render(Map.of("language", language, "statement", statement)))
                .stream()
                .content();
    }
    
    public Flux<String> promptTemplate4(String statement, String language) {    
		    String systemText = """
		        답변을 생성할 때 HTML와 CSS를 사용해서 파란 글자로 출력하세요.
		        <span> 태그 안에 들어갈 내용만 출력하세요.
		        """;
		    String userText = """
		        다음 한국어 문장을 %s로 번역해주세요.\n 문장: %s
		        """.formatted(language, statement);
		    
		    Flux<String> response = chatClient.prompt()
		        .system(systemText)
		        .user(userText)
		        .stream()
		        .content();
		    return response;
  }     
}
```

### 복수 메시지 추가

```java
@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerMultiMessages {
  // ##### 필드 ##### 
  @Autowired
  private AiServiceMultiMessages aiService;
  
  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    value = "/multi-messages",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String multiMessages(
      @RequestParam("question") String question, HttpSession session) {
    List<Message> chatMemory = (List<Message>) session.getAttribute("chatMemory");
    if(chatMemory == null) {
      chatMemory = new ArrayList<Message>();
      session.setAttribute("chatMemory", chatMemory);
    }
    String answer = aiService.multiMessages(question, chatMemory);
    return answer;
  }
}

@Service
@Slf4j
public class AiServiceMultiMessages {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceMultiMessages(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public String multiMessages(String question, List<Message> chatMemory) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
            당신은 AI 비서입니다.
            제공되는 지난 대화 내용을 보고 우선적으로 답변해주세요.
        """)
        .build();
    
    // 대화를 처음 시작할 경우, 시스템 메시지 저장
    if(chatMemory.size() == 0) {
      chatMemory.add(systemMessage);
    }
    
    // 이전 대화 내용 출력
    log.info(chatMemory.toString());
    
    // LLM에게 요청하고 응답받기
    ChatResponse chatResponse = chatClient.prompt()
        // 이전 대화 내용 추가
        .messages(chatMemory)
        // 사용자 메시지 추가
        .user(question)
        // 동기 방식으로 답변 얻기
        .call()
        // ChatResponse로 반환하기
        .chatResponse();
  
    // 대화 메시지 저장
    UserMessage userMessage = UserMessage.builder().text(question).build();
    chatMemory.add(userMessage);
    
    AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
    chatMemory.add(assistantMessage);
  
    // LLM의 텍스트 답변 반환
    String text = assistantMessage.getText();
    return text;
  }
}
```

## 프롬프트 엔지니어링

| 기본 원칙 | 설명 |
| --- | --- |
| 명확하고 구체적인 요청 | 프롬프트는 모호하지 않고 구체적이어야합니다. 원하는 답변의 범위와 방향을 명확히 정의해야합니다. |
| 모델의 이해를 돕는 배경 정보 제공 | LLM이 답변을 더 정확히 이해할 수 있도록 사용자 메시지에 배경 정보나 문맥을 제공합니다. |
| 간결하고 직관적인 문장 사용 | 모호하고 수식어가 많은 복잡한 문장보다는 간단하고 직관적인 문장을 사용합니다. |
| 적절한 예시 사용 | LLM이 사용자가 원하는 스타일이나 출력 형식을 정확히 이해할 수 있도록 예시를 포함하는 것이 좋습니다. |
| 다단계 질문 피하기 | 여러 질문을 한 프롬프트에 담지 말고 하나의 질문에 집중하여 LLM이 정확한 답변을 할 수 있도록 합니다. |
| LLM의 한계 이해 | LLM이 처리할 수 있는 범위와 한계를 이해하고 그에 맞는 프롬프트를 설계해야 합니다. |
| LLM의 역할 부여하기 | 모델에게 명확한 역할을 부여하여 특정 작업에 집중하게 합니다. |

### 프롬프트 엔지니어링 기법

- 제로-샷 프롬프트

  AI에게 예시 없이 작업을 수행하도록 요청하는 방법

  모델이 처음부터 지시를 이해하고 실행할 수 있는 능력이 있을 경우에 사용 가능

  LLM은 방대한 텍스트 데이터를 학습하여 ‘번역’, ‘요약’, ‘분류’와 같은 작업이 무엇인지 잘 알고 있기 때문에 명시적인 예시 없이도 작업을 잘 처리 가능

    ```java
    @Service
    @Slf4j
    public class AiServiceZeroShotPrompt {
      // ##### 필드 #####
      private ChatClient chatClient;
      private PromptTemplate promptTemplate = PromptTemplate.builder()
          .template("""
              영화 리뷰를 [긍정적, 중립적, 부정적] 중에서 하나로 분류하세요.
              레이블만 반환하세요.
              리뷰: {review}
            """)
          .build();
    
      // ##### 생성자 #####
      public AiServiceZeroShotPrompt(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder
            .defaultOptions(ChatOptions.builder()
                .temperature(0.0)
                .maxTokens(4)
                .build())
            .build();
      }
    
      // ##### 메소드 #####
      public String zeroShotPrompt(String review) {
        String sentiment = chatClient.prompt()
            .user(promptTemplate.render(Map.of("review", review)))
            .call()
            .content();
        return sentiment;
      }
    }
    ```

- 퓨-샷 프롬프트

  LLM에게 몇 개의 예시를 제공하여 사용자가 원하는 방식으로 출력하도록 유도하는 기법

  한 개의 예시를 제공하는 것을 원-샷

  LLM이 사용자가 원하는 형식을 학습하고, 이를 기반으로 새로운 질문에도 동일한 형식의 답변을 할 수 있습니다.

    ```java
    @Service
    @Slf4j
    public class AiServiceFewShotPrompt {
      // ##### 필드 #####
      private ChatClient chatClient;
    
      // ##### 생성자 #####
      public AiServiceFewShotPrompt(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder.build();
      }
    
      // ##### 메소드 #####
      public String fewShotPrompt(String order) {
        // 프롬프트 생성
        String strPrompt = """
            고객 주문을 유효한 JSON 형식으로 바꿔주세요.
            추가 설명은 포함하지 마세요.
    
            예시1:
            작은 피자 하나, 치즈랑 토마토 소스, 페퍼로니 올려서 주세요.
            JSON 응답:
            {
              "size": "small",
              "type": "normal",
              "ingredients": ["cheese", "tomato sauce", "pepperoni"]
            }
    
            예시1:
            큰 피자 하나, 토마토 소스랑 바질, 모짜렐라 올려서 주세요.
            JSON 응답:
            {
              "size": "large",
              "type": "normal",
              "ingredients": ["tomato sauce", "basil", "mozzarella"]
            }
    
            고객 주문: %s""".formatted(order);
    
        Prompt prompt = Prompt.builder()
            .content(strPrompt)
            .build();
    
        // LLM으로 요청하고 응답을 받음
        String pizzaOrderJson = chatClient.prompt(prompt)
            .options(ChatOptions.builder()
                .temperature(0.0)
                .maxTokens(300)
                .build())
            .call()
            .content();
    
        return pizzaOrderJson;
      }
    }
    ```

- 역할 부여 프롬프트

  LLM에게 특정 역할이나 인물을 맡도록 지시하면 출력 결과에 영향을 미침

  특정 정체성, 전문성 또는 관점을 부여함으로써 출력 내용의 스타일, 톤(진지한, 유머스러운), 깊이를 조정할 수 있음

    ```java
    @Service
    @Slf4j
    public class AiServiceRoleAssignmentPrompt {
      // ##### 필드 #####
      private ChatClient chatClient;
    
      // ##### 생성자 #####
      public AiServiceRoleAssignmentPrompt(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder.build();
      }
    
      // ##### 시스템 메시지를 활용해서 역할 부여하기 #####
      public Flux<String> roleAssignment(String requirements) {
        Flux<String> travelSuggestions = chatClient.prompt()
            // 시스템 메시지 추가
            .system("""
                당신이 여행 가이드 역할을 해 주었으면 좋겠습니다.
                아래 요청사항에서 위치를 알려주면, 근처에 있는 3곳을 제안해 주고,
                이유를 달아주세요. 경우에 따라서 방문하고 싶은 장소 유형을 
                제공할 수도 있습니다.
                출력 형식은 <ul> 태그이고, 장소는 굵게 표시해 주세요.
                """)
            // 사용자 메시지 추가
            .user("요청사항: %s".formatted(requirements))
            // 대화 옵션 설정
            .options(ChatOptions.builder()
                .temperature(1.0)
                .maxTokens(1000)
                .build())
            // LLM으로 요청하고 응답 얻기
            .stream()
            .content();
        return travelSuggestions;
      }
    }
    ```

- 스탭-백 프롬프트

  복잡한 질문을 여러 단계로 분해해, 단계별로 배경 지식을 확보하는 기법

  LLM이 즉각적인 답변을 생성하기 전에 “한 걸음 물러나” 문제와 관련된 폭넓은 배경 지식을 갖도록 유도

  단계별 질문에 대한 답변은 다음 질문의 배경 지식으로 이어지기 때문에 LLM은 단계적으로 배경 지식을 쌓아가며 더 정확한 답변을 제공

    ```java
  /**
    [질문]
    서올에서 을릉도로 걀 때 비용이 가장 적게 드는 방법은?
    
    [단계1]
    서울에서 울릉도로 가는 교통 수단은 무엇인가요?
    
    [단계2] 
    각 교통 수단의 비용은 얼마인가요?
    문맥: 단계1 답변 내용
    
    [단계3] 
    비용이 가장 적은 교웅 수단은 무엇인가요?
    문맥: 단계1 답변 + 단계2 답변
    
    [최종 처리]
    서울에서 울릉도로 갈 때 비용이 가장 적게 드는 방법은?
    문맥: 단계1 답변 + 단계2 답변 + 단계3 답변
  **/
    
    @Service
    @Slf4j
    public class AiServiceStepBackPrompt {
      // ##### 필드 #####
      private ChatClient chatClient;
    
      // ##### 생성자 #####
      public AiServiceStepBackPrompt(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder.build();
      }
    
      // ##### 메소드 #####
      public String stepBackPrompt(String question) throws Exception {
        String questions = chatClient.prompt()
            .user("""
                사용자 질문을 Step-Back 기법으로 재구성하세요.
    
    						요구사항:
    						1. 질문을 점진적으로 구체화하는 단계별 질문들로 분해하세요.
    						2. 마지막 질문은 반드시 사용자 질문과 동일해야 합니다.
    						3. 출력은 JSON 배열 형식만 허용됩니다.
    						4. 다른 설명은 포함하지 마세요.
    						
    						출력 형식 예시:
    						["질문1", "질문2", "질문3", "..."]
    						
    						사용자 질문: %s
                """.formatted(question))
            .call()
            .content();
      
        String json = questions.substring(questions.indexOf("["), questions.indexOf("]")+1);
        log.info(json);
        
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> listQuestion = objectMapper.readValue(
            json,
            new TypeReference<List<String>>() {}
        );
        
        String[] answerArray = new String[listQuestion.size()];
        for(int i=0; i<listQuestion.size(); i++) {
          String stepQuestion = listQuestion.get(i);
          String stepAnswer = getStepAnswer(stepQuestion, answerArray);
          answerArray[i] = stepAnswer;
          log.info("단계{} 질문: {}, 답변: {}", i+1, stepQuestion, stepAnswer);
        }
        
        return answerArray[answerArray.length-1];
      }
    
      public String getStepAnswer(String question, String... prevStepAnswers) {
        String context = "";
        for (String prevStepAnswer : prevStepAnswers) {
          context += Objects.requireNonNullElse(prevStepAnswer, "");
        }
        String answer = chatClient.prompt()
            .user("""
                %s
                문맥: %s
                """.formatted(question, context))
            .call()
            .content();
        return answer;
      }
    }
    ```

- 생각의 사슬 프롬프트

  LLM에게 문제를 해결하는 과정을 명시적으로 요청하거나 논리적인 단계로 생각하도록 요구함으로써, 다단계 추론이 필요한 작업에서 성능을 향상시킬 수 있음

  최종 답을 도출하기 전에 중간 추론 단계를 생성하도록 유도

  이는 인간이 복잡한 문제를 해결하는 방식과 유사하며, 모델의 사고 과정을 명확하게 만들고 더 정확한 결론에 도달할 수 있도록 돕는다.

  프롬프트에 “한 걸음씩 생각해 봅시다.(Let’s think step by step)”라는 핵심 문구를 넣어 모델이 자신의 사고 과정을 보여주도록 유도

    ```java
    @Service
    @Slf4j
    public class AiServiceChainOfThoughtPrompt {
      // ##### 필드 #####
      private ChatClient chatClient;
    
      // ##### 생성자 #####
      public AiServiceChainOfThoughtPrompt(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder.build();
      }
    
      // ##### 메소드 #####
      public Flux<String> chainOfThought(String question) {
        Flux<String> answer = chatClient.prompt()
            .user("""
                %s
                한 걸음씩 생각해 봅시다.
      
                [예시]
                질문: 제 동생이 2살일 때, 저는 그의 나이의 두 배였어요.
                지금 저는 40살인데, 제 동생은 몇 살일까요? 한 걸음씩 생각해 봅시다.
      
                답변: 제 동생이 2살일 때, 저는 2 * 2 = 4살이었어요.
                그때부터 2년 차이가 나며, 제가 더 나이가 많습니다.
                지금 저는 40살이니, 제 동생은 40 - 2 = 38살이에요. 정답은 38살입니다.
                """.formatted(question))
            .stream()
            .content();
        return answer;
      }
    }
    ```

- 자기 일관성

  LLM에게 여러 번 요청해서 얻은 응답을 집계하여 다수결로 최종 응답을 정하는 기법

  LLM이 일관성 있게 응답하는 것을 채택

  이 기법은 LLM 출력의 변동성 해결

    ```java
    @Service
    @Slf4j
    public class AiServiceSelfConsistency {
      // ##### 필드 #####
      private ChatClient chatClient;
      private PromptTemplate promptTemplate = PromptTemplate.builder()
          .template("""
              다음 내용을 [IMPORTANT, NOT_IMPORTANT] 둘 중 하나로 분류해 주세요.
              레이블만 반환하세요.
              내용: {content}
              """)
          .build();
    
      // ##### 생성자 #####
      public AiServiceSelfConsistency(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder.build();
      }
    
      // ##### 메소드 #####
      public String selfConsistency(String content) {
        int importantCount = 0;
        int notImportantCount = 0;
        
        String userText = promptTemplate.render(Map.of("content", content));
      
        // 다섯번에 걸쳐 응답 받아 보기
        for (int i = 0; i < 5; i++) {
          // LLM 요청 및 응답 받기
          String output = chatClient.prompt()
              .user(userText)
              .options(ChatOptions.builder()
                  .temperature(1.0)
                  .build())
              .call()
              .content();
      
          log.info("{}: {}", i, output.toString());
      
          // 결과 집계
          if (output.equals("IMPORTANT")) {
            importantCount++;
          } else {
            notImportantCount++;
          }
        }
      
        // 다수결로 최종 분류를 결정
        String finalClassification = importantCount > notImportantCount ?
                "중요함" : "중요하지 않음";
        return finalClassification;
      }
    }
    ```