## 구조화된 출력 변환기

일반적으로 LLM의 출력은 텍스트 문장. LLM이 구조화된 출력을 하려면 프롬프트에 출력 형식 지침을 포함시켜 올바른 JSON을 출력하도록 유도해야 함.

### StructuredOutputConverter<T>

- FormatProvider : 출력 형식 지침
- Converter<String, T> : LLM 출력 텍스트(String) → T

### ListOutputConverter

LLM이 쉼표로 구분된 텍스트 출력

```java
@Service
@Slf4j
public class AiServiceListOutputConverter {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceListOutputConverter(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public List<String> listOutputConverterLowLevel(String city) {
    // 구조화된 출력 변환기 생성
    ListOutputConverter converter = new ListOutputConverter();
    // 프롬프트 템플릿 생성
    PromptTemplate promptTemplate = PromptTemplate.builder()
        .template("{city}에서 유명한 호텔 목록 5개를 출력하세요. {format}")
        .build();
    // 프롬프트 생성
    Prompt prompt = promptTemplate.create(
        Map.of("city", city, "format", converter.getFormat()));
    // LLM의 쉼표로 구분된 텍스트 출력 얻기
    String commaSeparatedString = chatClient.prompt(prompt)
        .call()
        .content();
    // List<String>으로 변환
    List<String> hotelList = converter.convert(commaSeparatedString);
    return hotelList;
  }
  
  public List<String> listOutputConverterHighLevel(String city) {
    List<String> hotelList = chatClient.prompt()
        .user("%s에서 유명한 호텔 목록 5개를 출력하세요.".formatted(city))
        .call()
        .entity(new ListOutputConverter());
    return hotelList;
  }
}
```

### BeanOutputConverter<T>

LLM의 출력을 T 객체로 변환, LLM이 JSON 출력을 할 수 있도록 지침 생성하고, LLM 출력을 T 객체로 변환

```java
@Service
@Slf4j
public class AiServiceBeanOutputConverter {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceBeanOutputConverter(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public Hotel beanOutputConverterLowLevel(String city) {
    // 구조화된 출력 변환기 생성
    BeanOutputConverter<Hotel> beanOutputConverter = new BeanOutputConverter<>(Hotel.class);
    // 프롬프트 템플릿 생성
    PromptTemplate promptTemplate = PromptTemplate.builder()
        .template("{city}에서 유명한 호텔 목록 5개를 출력하세요. {format}")
        .build();
    // 프롬프트 생성
    Prompt prompt = promptTemplate.create(Map.of(
        "city", city,
        "format", beanOutputConverter.getFormat()));
    // LLM의 JSON 출력 얻기
    String json = chatClient.prompt(prompt)
        .call()
        .content();
    // JSON을 Hotel로 매핑해서 변환
    Hotel hotel = beanOutputConverter.convert(json);
    return hotel;
  }
  
  public Hotel beanOutputConverterHighLevel(String city) {
    Hotel hotel = chatClient.prompt()
        .user("%s에서 유명한 호텔 목록 5개를 출력하세요.".formatted(city))
        .call()
        .entity(Hotel.class);
    return hotel;
  }  
}
```

### BeanOutputConverter<List<T>>

LLM의 출력을 List<T> 객체로 변환, LLM이 JSON 출력을 할 수 있도록 지침 생성하고, LLM 출력을 List<T> 객체로 변환

```java
@Service
@Slf4j
public class AiServiceParameterizedTypeReference {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceParameterizedTypeReference(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public List<Hotel> genericBeanOutputConverterLowLevel(String cities) {
    // 구조화된 출력 변환기 생성
    BeanOutputConverter<List<Hotel>> beanOutputConverter = new BeanOutputConverter<>(
        new ParameterizedTypeReference<List<Hotel>>() {});
    // 프롬프트 템플릿 생성
    PromptTemplate promptTemplate = new PromptTemplate("""
        다음 도시들에서 유명한 호텔 3개를 출력하세요.
        {cities}
        {format}
        """);
    // 프롬프트 생성
    Prompt prompt = promptTemplate.create(Map.of(
        "cities", cities, 
        "format", beanOutputConverter.getFormat()));
    // LLM의 JSON 출력 얻기
    String json = chatClient.prompt(prompt)
        .call()
        .content();
    // JSON을 List<Hotel>로 매핑해서 변환
    List<Hotel> hotelList = beanOutputConverter.convert(json);
    return hotelList;
  }
  
  public List<Hotel> genericBeanOutputConverterHighLevel(String cities) {
    List<Hotel> hotelList = chatClient.prompt().user("""
        다음 도시들에서 유명한 호텔 3개를 출력하세요.
        %s
        """.formatted(cities))
        .call()
        .entity(new ParameterizedTypeReference<List<Hotel>>() {});
    return hotelList;
  }
}
```

### MapOutputConverter

LLM의 출력을 Map<String, Object> 객체로 변환

```java
@Service
@Slf4j
public class AiServiceMapOutputConverter {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceMapOutputConverter(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public Map<String, Object> mapOutputConverterLowLevel(String hotel) {
    // 구조화된 출력 변환기 생성
    MapOutputConverter mapOutputConverter = new MapOutputConverter();
    // 프롬프트 템플릿 생성
    PromptTemplate promptTemplate = new PromptTemplate(
        "호텔 {hotel}에 대해 정보를 알려주세요 {format}");
    // 프롬프트 생성
    Prompt prompt = promptTemplate.create(Map.of(
        "hotel", hotel,
        "format", mapOutputConverter.getFormat()));
    // LLM의 JSON 출력 얻기
    String json = chatClient.prompt(prompt)
        .call()
        .content();
    // List<String>으로 변환
    Map<String, Object> hotelInfo = mapOutputConverter.convert(json);
    return hotelInfo;
  }
  
  public Map<String, Object> mapOutputConverterHighLevel(String hotel) {
    Map<String, Object> hotelInfo = chatClient.prompt()
        .user("호텔 %s에 대해 정보를 알려주세요".formatted(hotel))
        .call()
        .entity(new MapOutputConverter());
    return hotelInfo;
  }

}
```

### 시스템 메시지와 함께 사용

```java
@Data
public class ReviewClassification {
  // ##### 열거 타입 선언 #####
  public enum Sentiment {
    POSITIVE, NEUTRAL, NEGATIVE
  }

  // ##### 필드 선언 #####
  private String review;
  private Sentiment classification;
}

@Service
@Slf4j
public class AiServiceSystemMessage {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceSystemMessage(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public ReviewClassification classifyReview(String review) {
    ReviewClassification reviewClassification = chatClient.prompt()
        .system("""
            영화 리뷰를 [POSITIVE, NEUTRAL, NEGATIVE] 중에서 하나로 분류하고,
            유효한 JSON을 반환하세요.
         """)
        .user("%s".formatted(review))
        .options(ChatOptions.builder().temperature(0.0).build())
        .call()
        .entity(ReviewClassification.class);
    return reviewClassification;
  }  
}
```