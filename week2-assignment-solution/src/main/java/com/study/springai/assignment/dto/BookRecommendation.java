package com.study.springai.assignment.dto;

public record BookRecommendation(
    String title,
    String author,
    String genre,
    String summary,
    int rating,
    String reason
) {
}
