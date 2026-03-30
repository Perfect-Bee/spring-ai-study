package com.study.week6.dto;

import java.util.Map;

public record DocumentAddRequest(
        String content,
        Map<String, Object> metadata
) {}
