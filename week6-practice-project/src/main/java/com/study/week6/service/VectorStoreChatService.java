package com.study.week6.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * VectorStore 대화 기억 서비스
 *
 * VectorStoreChatMemoryAdvisor를 사용하여 대화 기억을 벡터 저장소에 저장합니다.
 *
 * - 현재 대화와 의미적으로 유사한 이전 대화를 검색하여 프롬프트에 추가합니다.
 * - 대화의 양이 많고, 관련된 기억만 선택적으로 활용할 때 효과적입니다.
 * - 텍스트 임베딩 + 유사성 검색 과정으로 다른 방식보다 응답이 다소 느릴 수 있습니다.
 * - 별도 ChatMemory 구현체 없이 VectorStoreChatMemoryAdvisor만으로 관리합니다.
 * - chat_memory_vector_store 테이블을 사용합니다 (메인 vector_store 테이블과 분리).
 */
@Service
@Slf4j
public class VectorStoreChatService {

    private final ChatClient chatClient;

    public VectorStoreChatService(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            ChatModel chatModel) {

        // 대화 기억 전용 VectorStore (메인 VectorStore와 별도 테이블 사용)
        var chatMemoryVectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(false)  // SQL init 스크립트에서 이미 생성
                .schemaName("public")
                .vectorTableName("chat_memory_vector_store")
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        VectorStoreChatMemoryAdvisor.builder(chatMemoryVectorStore).defaultTopK(5).build(),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();
    }

    /**
     * VectorStore 기반 대화 기억을 사용한 채팅
     * 현재 대화와 유사한 이전 대화를 검색하여 컨텍스트에 추가합니다.
     */
    public String chat(String userText, String conversationId) {
        return chatClient.prompt()
                .user(userText)
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, conversationId
                ))
                .call()
                .content();
    }
}
