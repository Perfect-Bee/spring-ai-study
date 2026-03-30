package com.study.bookadvisor.dto;

import java.util.Map;

public record BookSearchResult(
        String documentId,
        String content,
        Map<String, Object> metadata,
        Double score
) {}
