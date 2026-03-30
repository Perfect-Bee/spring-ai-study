package com.study.week6.dto;

public record SearchRequest(
        String query,
        int topK,
        double similarityThreshold,
        String filterExpression
) {
    public SearchRequest {
        if (topK <= 0) topK = 4;
        if (similarityThreshold < 0) similarityThreshold = 0.0;
    }
}
