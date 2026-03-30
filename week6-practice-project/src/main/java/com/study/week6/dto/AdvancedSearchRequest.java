package com.study.week6.dto;

public record AdvancedSearchRequest(
        String query,
        Integer topK,
        Double similarityThreshold,
        String filterExpression
) {}
