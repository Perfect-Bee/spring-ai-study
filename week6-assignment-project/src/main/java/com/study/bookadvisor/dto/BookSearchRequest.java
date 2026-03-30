package com.study.bookadvisor.dto;

public record BookSearchRequest(
        String query,
        int topK,
        double similarityThreshold,
        String genre
) {
    public BookSearchRequest {
        if (topK <= 0) topK = 5;
        if (similarityThreshold < 0) similarityThreshold = 0.0;
    }
}
