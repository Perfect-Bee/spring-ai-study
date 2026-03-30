package com.study.week6.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

/**
 * RDBMS 대화 기억 서비스
 *
 * JdbcChatMemoryRepository + PromptChatMemoryAdvisor를 사용하여
 * PostgreSQL에 대화 기억을 영구 저장합니다.
 *
 * - 서버 재시작 후에도 대화 기억이 유지됩니다.
 * - PromptChatMemoryAdvisor: 대화 기억을 텍스트 형태로 시스템 메시지에 포함시킵니다.
 * - MessageWindowChatMemory: 최대 메시지 수를 제한하여 오래된 메시지를 자동 제거합니다.
 */
@Service
@Slf4j
public class JdbcChatService {

    private final ChatClient chatClient;

    public JdbcChatService(JdbcChatMemoryRepository chatMemoryRepository, ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(100)
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();
    }

    /**
     * JDBC 기반 대화 기억을 사용한 채팅
     * 대화 기억이 PostgreSQL에 영구 저장됩니다.
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
