package com.study.bookadvisor.service;

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
    private final VectorStore vectorStore;
    private final BookSearchService bookSearchService;

    public BookChatService(
            ChatModel chatModel,
            JdbcChatMemoryRepository chatMemoryRepository,
            VectorStore vectorStore,
            BookSearchService bookSearchService) {

        this.vectorStore = vectorStore;
        this.bookSearchService = bookSearchService;

        // ====================================================================
        // TODO 4: ChatClient에 Advisor Chain 구성
        // ====================================================================
        /**
         * [요구사항]
         * ChatClient를 빌드할 때 아래 3개의 Advisor를 defaultAdvisors로 등록하세요.
         *
         * 1) SafeGuardAdvisor
         *    - 차단할 민감 단어 목록: "욕설", "폭력", "도박", "불법 다운로드"
         *    - 차단 시 응답 메시지: "해당 질문은 도서 추천 서비스에서 다룰 수 없는 내용입니다."
         *    - 우선순위: Ordered.HIGHEST_PRECEDENCE (가장 먼저 실행)
         *
         * 2) PromptChatMemoryAdvisor
         *    - JdbcChatMemoryRepository를 사용하여 MessageWindowChatMemory를 빌드
         *    - maxMessages: 50 (최근 50개 메시지까지 기억)
         *    - PromptChatMemoryAdvisor.builder(chatMemory).build() 로 생성
         *
         * 3) SimpleLoggerAdvisor
         *    - 우선순위: Ordered.LOWEST_PRECEDENCE - 1 (가장 마지막에 실행)
         *
         * [힌트]
         * - SafeGuardAdvisor 생성: new SafeGuardAdvisor(List.of("단어들"), "차단메시지", 우선순위)
         * - ChatMemory 생성: MessageWindowChatMemory.builder()
         *     .chatMemoryRepository(chatMemoryRepository).maxMessages(50).build()
         * - ChatClient 빌드: ChatClient.builder(chatModel)
         *     .defaultSystem("시스템 프롬프트").defaultAdvisors(advisor1, advisor2, advisor3).build()
         */
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        당신은 AI 도서 추천 전문가입니다.
                        사용자의 관심사, 독서 수준, 선호 장르를 파악하여 적절한 도서를 추천해주세요.
                        추천할 때는 책 제목, 저자, 왜 추천하는지 이유를 함께 설명해주세요.
                        이전 대화 내용을 참고하여 일관성 있는 추천을 해주세요.
                        """)
                // TODO: .defaultAdvisors(...) 로 위의 3개 Advisor를 등록하세요
                .build();
    }

    // ========================================================================
    // TODO 5: 대화 기억을 활용한 멀티턴 도서 추천 채팅
    // ========================================================================
    /**
     * 대화 기억을 활용하여 멀티턴으로 도서를 추천합니다.
     *
     * [요구사항]
     * - 먼저 bookSearchService.searchSimple()로 사용자 질문과 유사한 도서를 검색
     * - 검색된 도서 정보를 "참고 도서 정보" 컨텍스트로 만들어서 프롬프트에 추가
     * - chatClient를 사용하여 LLM에 질문 전송
     * - advisorSpec.param()으로 ChatMemory.CONVERSATION_ID에 conversationId를 전달
     * - LLM 응답을 반환
     *
     * [힌트]
     * 1) 도서 검색: bookSearchService.searchSimple(userMessage)로 관련 도서 검색
     *
     * 2) 컨텍스트 생성: 검색 결과를 문자열로 조합
     *    StringBuilder context = new StringBuilder("\\n[참고할 수 있는 도서 목록]\\n");
     *    results.forEach(r -> context.append("- ").append(r.content()).append("\\n"));
     *
     * 3) ChatClient 호출:
     *    chatClient.prompt()
     *        .user(userMessage + context.toString())
     *        .advisors(advisorSpec -> advisorSpec.param(
     *            ChatMemory.CONVERSATION_ID, conversationId))
     *        .call()
     *        .content();
     *
     * @param userMessage    사용자 메시지
     * @param conversationId 대화 식별자 (세션별 구분)
     * @return LLM의 도서 추천 응답
     */
    public String chat(String userMessage, String conversationId) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO 5: 멀티턴 도서 추천 채팅을 구현하세요");
    }
}
