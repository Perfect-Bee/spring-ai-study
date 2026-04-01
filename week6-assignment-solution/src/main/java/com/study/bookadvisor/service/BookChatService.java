package com.study.bookadvisor.service;

import com.study.bookadvisor.dto.BookSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 도서 추천 채팅 서비스
 *
 * Advisor(로깅/가드) + Memory(멀티턴) + 시맨틱 도서 검색을 결합한
 * AI 도서 추천 채팅 서비스입니다.
 *
 * [아키텍처]
 * 사용자 질문
 *   → SafeGuardAdvisor (민감 단어 차단)
 *   → PromptChatMemoryAdvisor (이전 대화 기억 추가)
 *   → SimpleLoggerAdvisor (요청/응답 로깅)
 *   → LLM (도서 추천 응답 생성)
 *   → 사용자에게 응답 반환
 */
@Service
@Slf4j
public class BookChatService {

    private final ChatClient chatClient;
    private final BookSearchService bookSearchService;

    public BookChatService(
            ChatModel chatModel,
            JdbcChatMemoryRepository chatMemoryRepository,
            BookSearchService bookSearchService) {

        this.bookSearchService = bookSearchService;

        // 1) SafeGuardAdvisor - 민감 단어 차단 (가장 먼저 실행)
        SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(
                List.of("욕설", "폭력", "도박", "불법 다운로드"),
                "해당 질문은 도서 추천 서비스에서 다룰 수 없는 내용입니다.",
                Ordered.HIGHEST_PRECEDENCE
        );

        // 2) PromptChatMemoryAdvisor - JDBC 기반 대화 기억 (최근 50개 메시지)
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(50)
                .build();

        PromptChatMemoryAdvisor memoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory).build();

        // 3) SimpleLoggerAdvisor - 요청/응답 로깅 (가장 마지막에 실행)
        SimpleLoggerAdvisor loggerAdvisor = new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1);

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        당신은 AI 도서 추천 전문가입니다.
                        사용자의 관심사, 독서 수준, 선호 장르를 파악하여 적절한 도서를 추천해주세요.
                        추천할 때는 책 제목, 저자, 왜 추천하는지 이유를 함께 설명해주세요.
                        이전 대화 내용을 참고하여 일관성 있는 추천을 해주세요.
                        """)
                .defaultAdvisors(safeGuardAdvisor, memoryAdvisor, loggerAdvisor)
                .build();
    }

    /**
     * 대화 기억을 활용하여 멀티턴으로 도서를 추천합니다.
     *
     * 1) 사용자 질문으로 유사한 도서를 VectorStore에서 검색
     * 2) 검색 결과를 컨텍스트로 구성하여 프롬프트에 추가
     * 3) ChatMemory(JDBC)로 이전 대화 기억을 유지하며 LLM에 질문
     *
     * @param userMessage    사용자 메시지
     * @param conversationId 대화 식별자 (세션별 구분)
     * @return LLM의 도서 추천 응답
     */
    public String chat(String userMessage, String conversationId) {
        // 1) 시맨틱 검색으로 관련 도서 조회
        List<BookSearchResult> results = bookSearchService.searchSimple(userMessage);

        // 2) 검색 결과를 컨텍스트 문자열로 조합
        StringBuilder context = new StringBuilder();
        if (results != null && !results.isEmpty()) {
            context.append("\n[참고할 수 있는 도서 목록]\n");
            results.forEach(r -> context.append("- ").append(r.content()).append("\n"));
        }

        // 3) ChatClient로 LLM 호출 (SafeGuard → Memory → Logger Advisor Chain 자동 적용)
        return chatClient.prompt()
                .user(userMessage + context)
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
