package com.study.springai.assignment.service;

import com.study.springai.assignment.dto.BookAnalysisRequest;
import com.study.springai.assignment.dto.BookRecommendRequest;
import com.study.springai.assignment.dto.BookRecommendation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BookService {
    private final ChatClient chatClient;
    private final PromptTemplate bookAnalysisTemplate;

    public BookService(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.bookAnalysisTemplate = new PromptTemplate(new ClassPathResource("prompts/book-analysis.st"));
    }

    /**
     * TODO 1: 도서 추천 (Ch2 - 텍스트 대화)
     * ChatClient의 Fluent API를 사용하여 장르와 분위기에 맞는 도서를 추천합니다.
     */
    public String recommendBooks(BookRecommendRequest request) {
        return chatClient.prompt()
            .user(buildRecommendPrompt(request))
            .call()
            .content();
    }

    /**
     * TODO 2: 도서 분석 (Ch3 - 프롬프트 템플릿)
     * PromptTemplate과 외부 템플릿 파일을 사용하여 도서를 분석합니다.
     */
    public String analyzeBook(BookAnalysisRequest request) {
        String prompt = bookAnalysisTemplate.create(Map.of(
            "title", request.title(),
            "author", request.author()
        )).getContents();

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    /**
     * TODO 3: 구조화된 도서 추천 (Ch4 - 구조화된 출력)
     * entity() 메서드와 ParameterizedTypeReference를 사용하여 구조화된 응답을 반환합니다.
     */
    public List<BookRecommendation> getStructuredRecommendations(BookRecommendRequest request) {
        String prompt = buildRecommendPrompt(request) + "\n\n" +
            "각 도서에 대해 title(도서명), author(저자명), genre(장르), " +
            "summary(100자 이내 요약), rating(1~5 정수), reason(추천 이유)을 포함해주세요.";

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(new ParameterizedTypeReference<>() {});
    }

    /**
     * TODO 4: 제로-샷 도서 분류 (프롬프트 엔지니어링)
     * 예시 없이 명확한 지시문만으로 도서 설명의 장르를 분류합니다.
     */
    public String classifyBookZeroShot(String bookDescription) {
        String userPrompt = """
            다음 도서 설명을 읽고 장르를 분류해주세요.
            예시는 제공되지 않습니다. 도서 설명의 내용을 분석하여 가장 적합한 장르를 선택하세요.

            카테고리: 소설, 자기계발, 과학, 역사, 경영, IT, 에세이, 철학, 심리학, 예술

            도서 설명: "%s"

            다음 형식으로 응답하세요:
            - 분류 결과: [선택한 장르]
            - 신뢰도: [높음/중간/낮음]
            - 분류 근거: [왜 이 장르로 분류했는지 2-3문장으로 설명]
            - 관련 장르: [부차적으로 관련될 수 있는 장르 1-2개]
            """.formatted(bookDescription);

        return chatClient.prompt()
            .system("당신은 도서 분류 전문가입니다.")
            .user(userPrompt)
            .call()
            .content();
    }

    /**
     * TODO 5: 스텝-백 도서 분석 (프롬프트 엔지니어링)
     * 구체적 질문 전에 상위 개념을 먼저 탐색하는 단계적 분석을 수행합니다.
     */
    public String analyzeWithStepBack(String title, String question) {
        // 1차 호출: 상위 개념과 원리를 먼저 탐색
        String stepBackPrompt = """
            도서 "%s"에 대해 다음 질문과 관련된 상위 개념과 원리를 탐색해주세요.

            질문: "%s"

            이 도서와 질문에 관련된 핵심 개념, 장르적 맥락, 시대적 배경,
            문학적/학문적 원리를 정리해주세요.
            """.formatted(title, question);

        String stepBackResponse = chatClient.prompt()
            .user(stepBackPrompt)
            .call()
            .content();

        // 2차 호출: 1차 결과를 컨텍스트로 활용하여 최종 답변 생성
        String finalPrompt = """
            도서 "%s"에 대한 다음 질문에 답변해주세요.

            질문: "%s"

            다음은 이 질문과 관련하여 사전에 탐색한 상위 개념과 원리입니다:
            ---
            %s
            ---

            위의 상위 개념을 바탕으로 다음 형식으로 답변해주세요:

            ## 1단계: 상위 개념 탐색 (Step-Back)
            위에서 제공된 상위 개념을 요약 정리해주세요.

            ## 2단계: 상위 개념 적용
            상위 개념과 원리를 바탕으로, 원래 질문에 대한 답변을 구성해주세요.

            ## 3단계: 최종 답변
            위의 분석을 종합하여 명확하고 심층적인 최종 답변을 제시해주세요.
            """.formatted(title, question, stepBackResponse);

        return chatClient.prompt()
            .user(finalPrompt)
            .call()
            .content();
    }

    private String buildRecommendPrompt(BookRecommendRequest request) {
        return String.format(
            "다음 조건에 맞는 도서를 %d권 추천해주세요:\n" +
            "- 장르: %s\n" +
            "- 분위기: %s\n\n" +
            "각 도서의 제목, 저자, 출판연도, 간단한 설명을 포함하여 추천해주세요.",
            request.count(), request.genre(), request.mood()
        );
    }
}
