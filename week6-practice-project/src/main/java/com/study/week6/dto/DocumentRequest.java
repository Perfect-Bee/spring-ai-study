package com.study.week6.dto;

import java.util.Map;

public record DocumentRequest(
        String content,
        Map<String, Object> metadata
) {}
