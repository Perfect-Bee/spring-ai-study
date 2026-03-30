package com.study.week6.dto;

import java.util.Map;

public record DocumentResult(
        String id,
        String content,
        Map<String, Object> metadata,
        Double score
) {}
