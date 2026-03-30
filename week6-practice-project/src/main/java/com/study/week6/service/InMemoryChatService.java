package com.study.week6.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

/**
 * In-Memory 대화 기억 서비스
 *
 * MessageChatMemoryAdvisor + InMemoryChatMemoryRepository를 사용하여
 * 메모리 기반으로 대화 기억을 유지합니다.
 *
 * - 서버 재시작 시 대화 기억이 사라집니다 (휘발성).
 * - 빠른 응답 속도를 제공합니다.
 * - MessageChatMemoryAdvisor: 대화 기억을 메시지 모음(UserMessage + AssistantMessage)으로 프롬프트에 추가합니다.
 *
 * 주의: auto-config이 생성하는 ChatMemory 빈은 JDBC 설정이 있으면 JDBC 기반이므로,
 *       InMemory 동작을 보장하려면 InMemoryChatMemoryRepository를 직접 생성해야 합니다.
 */
@Service
@Slf4j
public class InMemoryChatService {

    private final ChatClient chatClient;

    public InMemoryChatService(ChatModel chatModel) {
        // InMemoryChatMemoryRepository를 명시적으로 생성하여 휘발성 보장
        ChatMemory inMemoryChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(inMemoryChatMemory).build(),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();
    }

    /**
     * In-Memory 대화 기억을 사용한 채팅
     * conversationId로 각 사용자/세션별 대화를 구분합니다.
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
