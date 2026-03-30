package com.study.week6.dto;

public record ChatRequest(
        String message,
        String conversationId
) {}
