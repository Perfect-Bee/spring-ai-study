package com.study.week6.controller;

import com.study.week6.dto.ChatRequest;
import com.study.week6.dto.ChatResponse;
import com.study.week6.service.InMemoryChatService;
import com.study.week6.service.JdbcChatService;
import com.study.week6.service.VectorStoreChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 대화 기억 API 컨트롤러
 *
 * 세 가지 대화 기억 방식을 비교 실습할 수 있습니다:
 * 1. In-Memory: 메모리 기반 (빠르지만 휘발성)
 * 2. JDBC: PostgreSQL 영구 저장
 * 3. VectorStore: 의미적 유사도 기반 기억 검색
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMemoryController {

    private final InMemoryChatService inMemoryChatService;
    private final JdbcChatService jdbcChatService;
    private final VectorStoreChatService vectorStoreChatService;

    /**
     * In-Memory 대화 기억 채팅
     */
    @PostMapping("/memory/inmemory")
    public ChatResponse chatInMemory(@RequestBody ChatRequest request) {
        String answer = inMemoryChatService.chat(request.message(), request.conversationId());
        return new ChatResponse(answer, request.conversationId());
    }

    /**
     * JDBC 대화 기억 채팅 (PostgreSQL 영구 저장)
     */
    @PostMapping("/memory/jdbc")
    public ChatResponse chatJdbc(@RequestBody ChatRequest request) {
        String answer = jdbcChatService.chat(request.message(), request.conversationId());
        return new ChatResponse(answer, request.conversationId());
    }

    /**
     * VectorStore 대화 기억 채팅 (유사도 기반 기억 검색)
     */
    @PostMapping("/memory/vectorstore")
    public ChatResponse chatVectorStore(@RequestBody ChatRequest request) {
        String answer = vectorStoreChatService.chat(request.message(), request.conversationId());
        return new ChatResponse(answer, request.conversationId());
    }
}
