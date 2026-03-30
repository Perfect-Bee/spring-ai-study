package com.study.bookadvisor.dto;

public record ChatRequest(
        String message,
        String conversationId
) {}
